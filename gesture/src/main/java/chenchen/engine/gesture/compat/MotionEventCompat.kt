package chenchen.engine.gesture.compat

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Build
import android.view.MotionEvent
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * [MotionEvent]兼容类，目的为了解决低版本无法使用`MotionEvent.getRawX(pointerIndex)`
 * @author: chenchen
 * @since: 2023/4/14 10:30
 */
class MotionEventCompat(val event: MotionEvent) {

    companion object {

        /**
         * 获取[getRawX]，[getRawY]相关的反射方法
         */
        private val getRawAxisValueMethod: Method by lazy {
            getNativeGetRawAxisValue().apply {
                isAccessible = true
            }
        }

        /**
         * 反射相关的字段
         */
        @delegate:SuppressLint("SoonBlockedPrivateApi")
        private val nativePtrField: Field by lazy {
            MotionEvent::class.java.getDeclaredField("mNativePtr").apply {
                isAccessible = true
            }
        }

        /**
         * 反射相关的字段
         */
        @delegate:SuppressLint("SoonBlockedPrivateApi")
        private val historyCurrentField: Field by lazy {
            MotionEvent::class.java.getDeclaredField("HISTORY_CURRENT").apply {
                isAccessible = true
            }
        }

        private fun getNativeGetRawAxisValue(): Method {
            for (method in MotionEvent::class.java.declaredMethods) {
                if (method.name.equals("nativeGetRawAxisValue")) {
                    method.isAccessible = true
                    return method
                }
            }
            throw RuntimeException("nativeGetRawAxisValue method not found.")
        }

        fun MotionEvent.compat(): MotionEventCompat {
            return MotionEventCompat(MotionEvent.obtain(this))
        }

        fun MotionEventCompat.obtain(): MotionEventCompat {
            return MotionEventCompat(MotionEvent.obtain(this.event))
        }
    }

    val x: Float
        get() = event.x


    val y: Float
        get() = event.y


    val rawX: Float
        get() = event.rawX

    val rawY: Float
        get() = event.rawY

    val pointerCount: Int
        get() = event.pointerCount

    val action: Int
        get() = event.action

    val actionIndex: Int
        get() = event.actionIndex

    val actionMasked: Int
        get() = event.actionMasked

    val actionButton: Int
        get() = event.actionButton

    fun getPointerId(pointerIndex: Int): Int {
        return event.getPointerId(pointerIndex)
    }

    fun findPointerIndex(pointerId: Int): Int {
        return event.findPointerIndex(pointerId)
    }

    fun getX(pointerIndex: Int): Float {
        return event.getX(pointerIndex)
    }

    fun getY(pointerIndex: Int): Float {
        return event.getY(pointerIndex)
    }

    fun getRawX(pointerIndex: Int): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.getRawX(pointerIndex)
            } else {
                getRawAxisValueMethod.invoke(
                    null, nativePtrField.get(event),
                    MotionEvent.AXIS_X, pointerIndex,
                    historyCurrentField.get(null)
                ) as Float
            }
        } catch (_: Exception) {
            event.getX(pointerIndex)
        }
    }

    fun getRawY(pointerIndex: Int): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.getRawY(pointerIndex)
            } else {
                getRawAxisValueMethod.invoke(
                    null, nativePtrField.get(event),
                    MotionEvent.AXIS_Y, pointerIndex,
                    historyCurrentField.get(null)
                ) as Float
            }
        } catch (_: Exception) {
            event.getY(pointerIndex)
        }
    }

    fun setLocation(x: Float, y: Float) {
        event.setLocation(x, y)
    }

    fun offsetLocation(deltaX: Float, deltaY: Float) {
        event.offsetLocation(deltaX, deltaY)
    }

    fun transform(matrix: Matrix) {
        event.transform(matrix)
    }

    fun applyTransform(matrix: Matrix) {
        event.transform(matrix)
    }

    fun recycle() {
        event.recycle()
    }
}

