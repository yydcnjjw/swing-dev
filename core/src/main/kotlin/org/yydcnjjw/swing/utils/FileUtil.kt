package org.yydcnjjw.swing.utils

import java.nio.file.Paths

fun String.getPath(dir: String) = Paths.get(this)
    .let {
        if (it.isAbsolute) {
            it
        } else {
            Paths.get(dir, this).toRealPath()
        }
    }
