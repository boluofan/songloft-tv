package com.songloft.tv.data.cache

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.concurrent.ConcurrentHashMap

/**
 * 剥离流开头的 ID3v2 标签，让 ExoPlayer 提取器能从偏移 0 识别真实容器。
 *
 * 背景：部分来源（如 bili 下载管线）会把含封面/歌词的 ID3v2 标签写进实际为
 * MP4/MOV 容器的文件并存成 .mp3，导致 Mp3Extractor（找不到 MPEG 帧）与
 * Mp4Extractor（偏移 0 不是 ftyp）都无法解析，报 UnrecognizedInputFormatException。
 * 本包装只对「开头是 ID3v2」的流生效：剥离标签后真实容器暴露给提取器；
 * 真 mp3（ID3 + MPEG 帧）剥离后仍可正常播放，无损。
 *
 * 探测结果按 URI 缓存在 [skipCache]：首次加载必从位置 0 打开并完成探测，
 * 后续 seek 打开的实例按缓存值换算坐标，避免重复探测。
 */
class Id3SkippingDataSource(
    private val upstream: DataSource,
    private val skipCache: ConcurrentHashMap<String, Long>
) : DataSource {

    private var pending = ByteArray(ID3_HEADER_SIZE)
    private var pendingLength = 0
    private var toDrain = 0L

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri.toString()
        skipCache[uri]?.let { skip ->
            // 已知标签长度：直接按干净坐标打开（length 相对 position，无需换算）
            pendingLength = 0
            toDrain = 0
            return upstream.open(dataSpec.buildUpon().setPosition(dataSpec.position + skip).build())
        }
        if (dataSpec.position == 0L) {
            val result = upstream.open(dataSpec)
            pendingLength = readUpTo(pending, 0, ID3_HEADER_SIZE)
            val tagSize = id3v2TagSize(pending, pendingLength)
            if (tagSize >= 0) {
                skipCache[uri] = ID3_HEADER_SIZE + tagSize
                toDrain = tagSize.toLong()
                pendingLength = 0
            }
            return result
        }
        // 未探测过就收到非零位置打开：正常流程不会出现（首次加载必从 0 开始），原样透传
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (pendingLength > 0) {
            val n = minOf(length, pendingLength)
            System.arraycopy(pending, 0, buffer, offset, n)
            System.arraycopy(pending, n, pending, 0, pendingLength - n)
            pendingLength -= n
            return n
        }
        if (toDrain > 0) {
            val drainBuffer = ByteArray(minOf(toDrain, DRAIN_CHUNK).toInt())
            while (toDrain > 0) {
                val n = upstream.read(drainBuffer, 0, minOf(drainBuffer.size, toDrain.toInt()))
                if (n == C.RESULT_END_OF_INPUT || n < 0) {
                    toDrain = 0
                    return C.RESULT_END_OF_INPUT
                }
                if (n == 0) break
                toDrain -= n
            }
        }
        return upstream.read(buffer, offset, length)
    }

    override fun close() = upstream.close()

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    /** 读满 [length] 字节或遇 EOF 为止 */
    private fun readUpTo(buffer: ByteArray, offset: Int, length: Int): Int {
        var read = 0
        while (read < length) {
            val n = upstream.read(buffer, offset + read, length - read)
            if (n == C.RESULT_END_OF_INPUT || n <= 0) break
            read += n
        }
        return read
    }

    /**
     * 探测 ID3v2 标签大小，非 ID3v2 返回 -1。
     * v2.2 用 4 字节大端长度，v2.3/v2.4 用 28 位 syncsafe；v2.4 带 footer 时追加 10 字节。
     */
    private fun id3v2TagSize(data: ByteArray, length: Int): Long {
        if (length < ID3_HEADER_SIZE) return -1
        if (data[0] != 0x49.toByte() || data[1] != 0x44.toByte() || data[2] != 0x33.toByte()) return -1
        val version = data[3].toInt() and 0xFF
        if (version !in 2..4) return -1
        val tagSize = if (version == 2) {
            ((data[6].toInt() and 0xFF) shl 24) or ((data[7].toInt() and 0xFF) shl 16) or
                ((data[8].toInt() and 0xFF) shl 8) or (data[9].toInt() and 0xFF)
        } else {
            ((data[6].toInt() and 0x7F) shl 21) or ((data[7].toInt() and 0x7F) shl 14) or
                ((data[8].toInt() and 0x7F) shl 7) or (data[9].toInt() and 0x7F)
        }
        val footer = if (version == 4 && (data[5].toInt() and 0x10) != 0) 10 else 0
        return tagSize + footer.toLong()
    }

    companion object {
        private const val ID3_HEADER_SIZE = 10
        private const val DRAIN_CHUNK = 8192L
    }
}
