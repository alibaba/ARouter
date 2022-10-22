package com.alibaba.android.arouter.compiler.ksp.utils

import com.alibaba.android.arouter.compiler.utils.Consts
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KSPLoggerWrapper(private val logger: KSPLogger) : KSPLogger {
    companion object {
        const val TAG = Consts.PROJECT + "::Compiler "
    }

    override fun error(message: String, symbol: KSNode?) {
        if (message.isNotEmpty()) {
            logger.error("${TAG}${message}", symbol)
        }
    }

    override fun exception(e: Throwable) {
        logger.exception(e)
    }

    /**
     * When dev, please use logger.warn to print logo at terminal
     * more detail can see this issue
     * (No messages displayed when I use KSPLogger.logging and info.)[https://github.com/google/ksp/issues/1111]
     * */
    override fun info(message: String, symbol: KSNode?) {
        if (message.isNotEmpty()) {
            logger.info("${TAG}${message}", symbol)
        }
    }

    override fun logging(message: String, symbol: KSNode?) {
        if (message.isNotEmpty()) {
            logger.logging("${TAG}${message}", symbol)
        }
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (message.isNotEmpty()) {
            logger.warn("${TAG}${message}", symbol)
        }
    }
}