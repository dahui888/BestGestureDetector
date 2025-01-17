package chenchen.engine.gesture.adsorption

import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import chenchen.engine.gesture.ConstrainedAlignment
import chenchen.engine.gesture.ConstrainedAlignment.*
import chenchen.engine.gesture.ConstraintAlignment
import chenchen.engine.gesture.MovementTrack
import chenchen.engine.gesture.getGlobalRect
import kotlin.math.abs

/**
 * @author: chenchen
 * @since: 2023/4/19 18:20
 * @Description: 吸附
 */
data class Adsorption(
    /**
     * 磁性物体
     */
    val magnetic: Magnetic,
    /**
     * 磁铁列表
     */
    val magnets: List<Magnet>,
) {

    /**
     * 吸附动画
     */
    internal var adsorptionValueAnim: ValueAnimator? = null

    /**
     * 磁性物体的坐标，复用
     */
    internal val magneticRect = Rect()

    /**
     * 磁铁的坐标，复用
     */
    internal val magnetRect = Rect()

    /**
     * 吸住左边的磁铁
     */
    internal var leftAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 吸住水平居中的磁铁
     */
    internal var horizontalCenterAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 吸住右边的磁铁
     */
    internal var rightAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 吸住顶部的磁铁
     */
    internal var topAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 吸住垂直居中的磁铁
     */
    internal var verticalCenterAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 吸住底部的磁铁
     */
    internal var bottomAnalyzes = ArrayList<AnalyzeResult>()

    /**
     * 上次被吸住的水平运动轨迹
     */
    internal var hMovementTrack: MovementTrack = MovementTrack.None

    /**
     * 上次被吸住的垂直运动轨迹
     */
    internal var vMovementTrack: MovementTrack = MovementTrack.None

    /**
     * X轴是否吸附，吸附之后需要挣脱，X轴吸附后可以自由移动Y轴
     */
    internal var isAdsorptionH = false
        set(value) {
            field = value
            if (field) {
                //找出磁性最大的磁铁，以它的挣脱值为准
                pendingConsumeHRidThreshold = getMaxHRidThreshold()
            }
        }

    /**
     * Y轴是否吸附，吸附之后需要挣脱，Y轴吸附后可以自由移动X轴
     */
    internal var isAdsorptionV = false
        set(value) {
            field = value
            if (field) {
                //找出磁性最大的磁铁，以它的挣脱值为准
                pendingConsumeVRidThreshold = getMaxVRidThreshold()
            }
        }

    /**
     * 是否处于X轴免疫吸附的区域
     */
    internal var isAdsorptionImmunityRegionH = false

    /**
     * 是否处于Y轴免疫吸附的区域
     */
    internal var isAdsorptionImmunityRegionV = false

    /**
     * 待挣脱的X轴值
     */
    internal var pendingConsumeHRidThreshold = 0f
        set(value) {
            field = value
            if (field <= 0f) {
                //步骤5.3 挣脱后是处于免疫区
                field = 0f
                isAdsorptionH = false
                isAdsorptionImmunityRegionH = true
                //步骤5.4 找出磁性最大的磁铁，以它的免疫区为准
                pendingConsumeHImmunityThreshold = getMaxHImmunityThreshold()
            }
        }

    /**
     * 待挣脱的Y轴值
     */
    internal var pendingConsumeVRidThreshold = 0f
        set(value) {
            field = value
            //挣脱后是处于免疫区
            if (field <= 0f) {
                field = 0f
                isAdsorptionV = false
                isAdsorptionImmunityRegionV = true
                //步骤5.4 找出磁性最大的磁铁，以它的免疫区为准
                pendingConsumeVImmunityThreshold = getMaxVImmunityThreshold()
            }
        }

    /**
     * 挣脱X轴后的免疫区域值
     */
    internal var pendingConsumeHImmunityThreshold = 0f
        set(value) {
            field = value
            //脱离免疫区后就可以再次被吸附了
            if (field <= 0f) {
                //步骤 6.3 脱离免疫区
                field = 0f
                isAdsorptionImmunityRegionH = false
            }
        }

    /**
     * 挣脱Y轴后的免疫区域值
     */
    internal var pendingConsumeVImmunityThreshold = 0f
        set(value) {
            field = value
            //脱离免疫区后就可以再次被吸附了
            if (field <= 0f) {
                //步骤 6.3 脱离免疫区
                field = 0f
                isAdsorptionImmunityRegionV = false
            }
        }

    /**
     * 获取被吸住的磁铁记录
     */
    private fun getMaxAdsorptionMeasureResult(vararg measures: List<AnalyzeResult>): AnalyzeResult? {
        val maxMeasures = arrayListOf<AnalyzeResult>()
        for (measure in measures) {
            measure.filter { it.magnet.target.isAttachedToWindow }
                .maxByOrNull { it.magnet.hMagnetismThreshold }
                ?.apply { maxMeasures.add(this) }
        }
        return maxMeasures.maxByOrNull { it.magnet.hMagnetismThreshold }
    }

    /**
     * 获取磁性最大的磁铁
     */
    private fun getMaxAdsorptionThresholdMagnet(vararg measures: List<AnalyzeResult>): Magnet? {
        return getMaxAdsorptionMeasureResult(*measures)?.magnet
    }

    /**
     * 获取x轴最大的挣脱值
     */
    private fun getMaxHRidThreshold(): Float {
        //找出磁性最大的磁铁，以它的挣脱值为准
        val magnet = getMaxAdsorptionThresholdMagnet(leftAnalyzes, horizontalCenterAnalyzes, rightAnalyzes)
        return magnet?.hRidThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取y轴最大的挣脱值
     */
    private fun getMaxVRidThreshold(): Float {
        //找出磁性最大的磁铁，以它的挣脱值为准
        val magnet = getMaxAdsorptionThresholdMagnet(topAnalyzes, verticalCenterAnalyzes, bottomAnalyzes)
        return magnet?.vRidThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取x轴最大的免疫区
     */
    private fun getMaxHImmunityThreshold(): Float {
        //找出磁性最大的磁铁，以它的免疫区为准
        val magnet = getMaxAdsorptionThresholdMagnet(leftAnalyzes, horizontalCenterAnalyzes, rightAnalyzes)
        return magnet?.hImmunityThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取y轴最大的免疫区
     */
    private fun getMaxVImmunityThreshold(): Float {
        //找出磁性最大的磁铁，以它的免疫区为准
        val magnet = getMaxAdsorptionThresholdMagnet(topAnalyzes, verticalCenterAnalyzes, bottomAnalyzes)
        return magnet?.vImmunityThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取x轴被吸住的磁铁和磁性物体的对齐方式的距离
     */
    private fun getHAlignmentDistance(): Int {
        if (!magnetic.target.isAttachedToWindow) {
            return 0
        }
        magneticRect.setEmpty()
        magnetRect.setEmpty()
        val measureResult = getMaxAdsorptionMeasureResult(
            leftAnalyzes, horizontalCenterAnalyzes, rightAnalyzes
        ) ?: return 0
        magnetic.target.getGlobalRect(magneticRect)
        measureResult.magnet.target.getGlobalRect(magnetRect)
        val adsRect = magneticRect
        val magnetRect = magnetRect
        return when (measureResult.alignment) {
            LeftToLeft -> adsRect.left - magnetRect.left
            LeftToHorizontalCenter -> adsRect.left - magnetRect.centerX()
            LeftToRight -> adsRect.left - magnetRect.right
            HorizontalCenterToLeft -> adsRect.centerX() - magnetRect.left
            HorizontalCenterToHorizontalCenter -> adsRect.centerX() - magnetRect.centerX()
            HorizontalCenterToRight -> adsRect.centerX() - magnetRect.right
            RightToLeft -> adsRect.right - magnetRect.left
            RightToHorizontalCenter -> adsRect.right - magnetRect.centerX()
            RightToRight -> adsRect.right - magnetRect.right
            else -> 0
        }.apply {
            magneticRect.setEmpty()
            this@Adsorption.magnetRect.setEmpty()
        }
    }

    /**
     * 获取y轴被吸住的磁铁和磁性物体的对齐方式的距离
     */
    private fun getVAlignmentDistance(): Int {
        if (!magnetic.target.isAttachedToWindow) {
            return 0
        }
        magneticRect.setEmpty()
        magnetRect.setEmpty()
        val measureResult = getMaxAdsorptionMeasureResult(
            topAnalyzes, verticalCenterAnalyzes, bottomAnalyzes
        ) ?: return 0
        magnetic.target.getGlobalRect(magneticRect)
        measureResult.magnet.target.getGlobalRect(magnetRect)
        val adsRect = magneticRect
        val magnetRect = magnetRect
        return when (measureResult.alignment) {
            TopToTop -> adsRect.top - magnetRect.top
            TopToVerticalCenter -> adsRect.top - magnetRect.centerY()
            TopToBottom -> adsRect.top - magnetRect.bottom
            VerticalCenterToTop -> adsRect.centerY() - magnetRect.top
            VerticalCenterToVerticalCenter -> adsRect.centerY() - magnetRect.centerY()
            VerticalCenterToBottom -> adsRect.centerY() - magnetRect.bottom
            BottomToTop -> adsRect.bottom - magnetRect.top
            BottomToVerticalCenter -> adsRect.bottom - magnetRect.centerY()
            BottomToBottom -> adsRect.bottom - magnetRect.bottom
            else -> 0
        }.apply {
            magneticRect.setEmpty()
            this@Adsorption.magnetRect.setEmpty()
        }
    }

    /**
     * 步骤5.1 X轴被吸附上了，需要挣脱
     */
    fun consumeHRidThreshold(moveX: Float) {
        //步骤5.1 必须吸附才消费挣脱
        if (isAdsorptionH) {
            //步骤5.2 判断吸附时的手势，如果是反方向就消费，如果换了一遍方向，就换手势
            when (hMovementTrack) {
                MovementTrack.LeftToRight -> {
                    if (moveX < 0) {
                        //步骤5.2.1.1 手势是从左往右被吸附的，那么往左移动就消费挣脱值
                        pendingConsumeHRidThreshold += moveX
                    } else if (moveX > 0) {
                        //步骤5.2.1.2 手势是从左往右被吸附的，继续往右移动，切换手势变成从右往左
                        hMovementTrack = MovementTrack.RightToLeft
                        pendingConsumeHRidThreshold = getMaxHRidThreshold()
                    }
                }
                MovementTrack.RightToLeft -> {
                    if (moveX < 0) {
                        //步骤5.2.1.2 手势是从右往左被吸附的，继续往左移动，切换手势变成从左往右
                        hMovementTrack = MovementTrack.LeftToRight
                        pendingConsumeHRidThreshold = getMaxHRidThreshold()
                    } else if (moveX > 0) {
                        //步骤5.2.1.1 手势是从右往左被吸附的，那么往右移动就消费挣脱值
                        pendingConsumeHRidThreshold -= moveX
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * Y轴被吸附上了，需要挣脱，挣脱后是处于免疫区
     */
    fun consumeVRidThreshold(moveY: Float) {
        if (isAdsorptionV) {
            when (vMovementTrack) {
                MovementTrack.TopToBottom -> {
                    if (moveY < 0) {
                        pendingConsumeVRidThreshold += moveY
                    } else if (moveY > 0) {
                        vMovementTrack = MovementTrack.BottomToTop
                        pendingConsumeVRidThreshold = getMaxVRidThreshold()
                    }
                }
                MovementTrack.BottomToTop -> {
                    if (moveY < 0) {
                        vMovementTrack = MovementTrack.TopToBottom
                        pendingConsumeVRidThreshold = getMaxVRidThreshold()
                    } else if (moveY > 0) {
                        pendingConsumeVRidThreshold -= moveY
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * 步骤6 X轴处于免疫区，免疫区内部不会再次吸附，需要脱离免疫区才能吸附
     *
     * 坑：这里使用[getHAlignmentDistance]判断免疫区是，因为使用手势在免疫区移动的时候，会出现免疫区消费完但[View]并没有移出免疫区
     * 原因是因为[View]的移动不止通过[View.setX]，也可以通过修改[View.setLeft]、[View.setRight]修改，
     * 但[View.setLeft]、[View.setRight]需要提供[Int]类型的值，每次将移动的[Float]转成[Int]都会丢失部分值，
     * 所以[View]实际上并没有移动那么多，但我们记录的免疫区值并没有丢失精度，两个数值比较就出现了偏差。
     * 如果用[View.setX]移动，则不需要通过[getHAlignmentDistance]来判断，
     * 用`adsorptionViewRect.left - magnetViewRect.left`判断一下位置差距即可
     */
    fun consumeHImmunityThreshold(moveX: Float) {
        //步骤6.1 必须处于免疫区才消费
        if (isAdsorptionImmunityRegionH) {
            //步骤6.2 判断吸附时的手势
            when (hMovementTrack) {
                MovementTrack.LeftToRight -> {
                    if (moveX < 0) {
                        //6.2.1 手势是从左往右被吸附的，那么往左移动就脱离免疫区，这里没有去消费moveX，原因上面注释写了
                        val distance = getHAlignmentDistance()
                        if (abs(distance) > pendingConsumeHImmunityThreshold) {
                            pendingConsumeHImmunityThreshold = 0f
                        }
                    } else if (moveX > 0) {
                        //6.2.2 手势是从左往右被吸附的，继续往右移动，切换手势变成从右往左
                        hMovementTrack = MovementTrack.RightToLeft
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeHImmunityThreshold = 0f
                    }
                }
                MovementTrack.RightToLeft -> {
                    if (moveX < 0) {
                        //6.2.2 手势是从右往左被吸附的，继续往左移动，切换手势变成从左往右
                        hMovementTrack = MovementTrack.LeftToRight
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeHImmunityThreshold = 0f
                    } else if (moveX > 0) {
                        //6.2.1 手势是从右往左被吸附的，那么往右移动就脱离免疫区，这里没有去消费moveX，原因上面注释写了
                        val distance = getHAlignmentDistance()
                        if (abs(distance) > pendingConsumeHImmunityThreshold) {
                            pendingConsumeHImmunityThreshold = 0f
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    fun consumeVImmunityThreshold(moveY: Float) {
        if (isAdsorptionImmunityRegionV) {
            when (vMovementTrack) {
                MovementTrack.TopToBottom -> {
                    if (moveY < 0) {
                        val distance = getVAlignmentDistance()
                        if (abs(distance) > pendingConsumeVImmunityThreshold) {
                            pendingConsumeVImmunityThreshold = 0f
                        }
                    } else if (moveY > 0) {
                        vMovementTrack = MovementTrack.BottomToTop
                        pendingConsumeVImmunityThreshold = 0f
                    }
                }
                MovementTrack.BottomToTop -> {
                    if (moveY < 0) {
                        vMovementTrack = MovementTrack.TopToBottom
                        pendingConsumeVImmunityThreshold = 0f
                    } else if (moveY > 0) {
                        val distance = getVAlignmentDistance()
                        if (abs(distance) > pendingConsumeVImmunityThreshold) {
                            pendingConsumeVImmunityThreshold = 0f
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

/**
 * 磁性物体
 */
class Magnetic(
    /**
     * 有磁性的View，可以被磁吸
     */
    val target: View,
    /**
     * 磁性物体的受约束方式
     */
    val alignments: List<ConstrainedAlignment>,
)

/**
 * 磁铁
 */
class Magnet(
    /**
     * 磁铁View，可以磁吸磁性物体
     */
    val target: View,
    /**
     * 磁铁的约束方式
     */
    val alignments: List<ConstraintAlignment>,
    /**
     * 垂直的磁性阈值，比如触发Top、VerticalCenter、Bottom吸附时的阈值
     */
    val vMagnetismThreshold: Int = 20,
    /**
     * 水平的磁性阈值，比如触发Left、HorizontalCenter、Right吸附时的阈值
     */
    val hMagnetismThreshold: Int = 20,
    /**
     * 垂直的挣脱阈值，比如挣脱Top、VerticalCenter、Bottom的阈值
     */
    val vRidThreshold: Int = vMagnetismThreshold * 2,
    /**
     * 水平挣脱阈值，比如挣脱Left、HorizontalCenter、Right的阈值
     */
    val hRidThreshold: Int = hMagnetismThreshold * 2,
    /**
     * 垂直挣脱吸附后，在一定区间内不会再吸附回去的阈值
     */
    val vImmunityThreshold: Int = 20,
    /**
     * 水平挣脱吸附后，在一定区间内不会再吸附回去的阈值
     */
    val hImmunityThreshold: Int = 20,
)


/**
 * 测量结果
 */
data class AnalyzeResult(
    /**
     * 磁性物体与磁铁的距离
     */
    val distance: Int,
    /**
     * 磁性物体与磁铁的对齐方式
     */
    val alignment: ConstrainedAlignment,
    /**
     * 磁铁
     */
    val magnet: Magnet
)