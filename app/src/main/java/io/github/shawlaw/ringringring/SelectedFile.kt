package io.github.shawlaw.ringringring

data class SelectedFile(val path: String, val name:String,
                        val size: Int, val mimeType: String,
                        val title: String, val artist: String,
                        val duration: Long, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectedFile

        if (path != other.path) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (mimeType != other.mimeType) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (duration != other.duration) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    fun toReadeableString(): String {
        return "FileName: $name\nFilePath: $path\nFileSize: $size Byte\n" +
                "MimeType: $mimeType\nSongTitle: $title\nArtist: $artist\nTotalDuration: ${duration/1_000F}s"
    }

    fun isValid(): Boolean {
        return true
    }
}