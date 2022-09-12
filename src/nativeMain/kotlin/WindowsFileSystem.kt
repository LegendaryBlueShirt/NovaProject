import kotlinx.cinterop.*
import okio.*
import okio.Path.Companion.toPath
import platform.posix.*

object WindowsFileSystem: FileSystem() {
    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        return WindowsSink(openReadWrite(file, false, mustExist))
    }

    override fun atomicMove(source: Path, target: Path) {
        if(source == target) {
            return
        }
        delete(target, false)
        val ret = rename(source.toString(), target.toString())
        if(ret != 0) {
            throw IOException("Failed to move file $source to $target")
        }
    }

    override fun canonicalize(path: Path): Path {
        if(path.isAbsolute) {
            return path
        }
        val buffer = MemScope().allocArray<ByteVar>(PATH_MAX)
        getcwd(buffer, PATH_MAX)
        val currentDir = buffer.toKString().toPath()
        return currentDir.div(path)
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        val ret = mkdir(dir.toString())
        if(ret != 0 && mustCreate) {
            throw IOException("Failed to create directory $dir")
        }
    }

    override fun createSymlink(source: Path, target: Path) {
        TODO("Not yet implemented")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if(exists(path)) {
            val ret = unlink(path.toString())
            if(ret != 0) {
                throw IOException("Error deleting file $path")
            }
        } else if(mustExist) {
            throw IOException("Failed to delete nonexistent file $path")
        }
    }

    override fun list(dir: Path): List<Path> {
        val dp = opendir(dir.toString())?:throw IOException("Directory $dir could not be opened")
        val returnList = mutableListOf<Path>()
        do {
            val ep = readdir(dp)
            ep?.let {
                returnList.add(dir.div(it.pointed.d_name.toKString()))
            }
        } while (ep != null)
        return returnList
    }

    override fun listOrNull(dir: Path): List<Path>? {
        val dp = opendir(dir.toString())?:return null
        val returnList = mutableListOf<Path>()
        do {
            val ep = readdir(dp)
            ep?.let {
                returnList.add(dir.div(it.pointed.d_name.toKString()))
            }
        } while (ep != null)
        return returnList
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        memScoped {
            val info = alloc<stat>()
            if (stat(path.toString(), info.ptr) != 0) return null
            return when(info.st_mode.toInt() and S_IFMT) {
                S_IFREG -> FileMetadata(
                    isRegularFile = true,
                    isDirectory = false,
                    symlinkTarget = null,
                    size = info.st_size.toLong(),
                    lastAccessedAtMillis = info.st_atime,
                    lastModifiedAtMillis = info.st_mtime)
                S_IFDIR -> FileMetadata(
                    isRegularFile = false,
                    isDirectory = true,
                    symlinkTarget = null)
                else -> null
            }
        }
    }

    override fun openReadOnly(file: Path): FileHandle {
        val f = fopen(file.toString(), "rb")?: throw IOException("Cannot open file for reading $file Error $errno")
        return WindowsFileHandle(f, false)
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        val f = fopen(file.toString(), "wb+") ?: throw IOException("Cannot open file for writing $file Error $errno")
        return WindowsFileHandle(f, true)
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        return WindowsSink(openReadWrite(file, mustCreate, false))
    }

    override fun source(file: Path): Source {
        return WindowsSource(openReadOnly(file))
    }
}

class WindowsFileHandle(val filePointer: CPointer<FILE>, readWrite: Boolean): FileHandle(readWrite) {
    val fileSize: Long by lazy {
        memScoped {
            val fd = fileno(filePointer)
            val info = alloc<stat>()
            fstat(fd, info.ptr)
            info.st_size.toLong()
        }
    }

    override fun protectedClose() {
        if(fclose(filePointer) != 0) {
            throw IOException("Failed to close filepointer! Error $errno")
        }
    }

    override fun protectedFlush() {
        fflush(filePointer)
    }

    override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int {
        if(fileOffset == fileSize)
            return -1
        fseek(filePointer, fileOffset.toInt(), SEEK_SET)
        return fread(array.refTo(arrayOffset), 1L.toULong(), byteCount.toULong(), filePointer).toInt()
    }

    override fun protectedResize(size: Long) {
        val fd = fileno(filePointer)
        ftruncate(fd, size.toInt())
    }

    override fun protectedSize(): Long {
        return fileSize
    }

    override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) {
        fwrite(array.refTo(arrayOffset), 1L.toULong(), byteCount.toULong(), filePointer)
    }
}

class WindowsSource(val handle: FileHandle): Source {
    override fun close() {
        handle.close()
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        return handle.read(handle.position(this), sink, byteCount)
    }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }
}

class WindowsSink(val handle: FileHandle): Sink {
    override fun close() {
        handle.close()
    }

    override fun flush() {
        handle.flush()
    }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }

    override fun write(source: Buffer, byteCount: Long) {
        handle.write(handle.position(source as Source), source, byteCount)
    }
}

actual fun getFileSystem(): FileSystem {
    return WindowsFileSystem
}