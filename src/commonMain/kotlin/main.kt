import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Key
import com.soywiz.korge.*
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.resources.resource
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.*
import kotlin.math.abs

const val SIZE_LINE = 0
const val COUNT_LINE = 1
const val FIGURES_START = 2

const val FIGURE_TYPE_RECTANGLE = "rectangle"
const val FIGURE_TYPE_CIRCLE = "circle"

const val ANIMATION_TYPE_MOVE = "move"
const val ANIMATION_TYPE_ROTATE = "rotate"
const val ANIMAtION_TYPE_SCALE = "scale"

var isContinue: Boolean = false
var isStop: Boolean
	set(value) { isContinue = !value }
	get() = !isContinue
var speed: Double = 0.125

suspend fun main() {
	fun List<String>.paramsLine(index: Int): List<String> = get(index).split(" ")

	val file = resource("10.txt").readLines()

	val (windowWidth, windowHeight) = file.paramsLine(SIZE_LINE).map { it.toInt() }

	Korge(width = windowWidth, height = windowHeight, bgcolor = Colors["#888"]) {
		val figureCount = file[COUNT_LINE].toInt()
		var currentFigure: View
		var animationCount: Int
		var curLine = FIGURES_START
		for (figureNumber in 1..figureCount) {
			currentFigure = file.paramsLine(curLine++).run {
				createFigure(type, args)
			}

			animationCount = file[curLine++].toInt()
			for (figureAnimation in 1..animationCount) {
				file.paramsLine(curLine++).run {
					currentFigure.addAnimation(type, args)
				}
			}
		}

		onClick {
			isContinue = !isContinue
		}

		onKeyDown {
			when (it.key) {
				Key.UP -> speed *= 2
				Key.DOWN -> speed /= 2
				else -> Unit
			}
		}
	}
}

fun Stage.createFigure(type: String, args: List<String>): View {
	return when(type) {
		FIGURE_TYPE_RECTANGLE -> createRectangle(args.centerX, args.centerY, args.width, args.height, args.angle, args.color)
		FIGURE_TYPE_CIRCLE -> createCircle(args.centerX, args.centerY, args.radius, args.color)
		else -> throw Exception("Wrong type")
	}
}

fun Stage.createRectangle(centerX: Double, centerY: Double, width: Double, height: Double, angle: Angle, color: RGBA): SolidRect {
	return solidRect(width, height, color) {
		anchor(0.5, 0.5)
		position(centerX, centerY)
		rotation(angle)
	}
}

fun Stage.createCircle(centerX: Double, centerY: Double, radius: Double, color: RGBA): Circle {
	return circle(radius, color) {
		anchor(0.5, 0.5)
		position(centerX, centerY)
	}
}

fun View.addAnimation(type: String, args: List<String>) {
	addUpdater(toStoppableUpdater(
		when(type) {
			ANIMATION_TYPE_MOVE -> moveAnimation(args.destX, args.destY, args.time, args.isCycle)
			ANIMATION_TYPE_ROTATE -> rotateAnimation(args.rotateAngle, args.time, args.isCycle)
			ANIMAtION_TYPE_SCALE -> scaleAnimation(args.destScale, args.time, args.isCycle)
			else -> throw Exception("Wrong type")
		}
	))
}

fun View.moveAnimation(destX: Double, destY: Double, time: TimeSpan, isCycle: Boolean): View.(TimeSpan) -> Unit {
	var start = pos.copy()
	var end = Point(destX, destY)
	val dist = distance(start, end) as Double
	var delta = end - start

	return { dt ->
		val part = dt / time

		val remainPart = (distance(pos, end) as Double) / dist
		if (remainPart > part) {
			pos += (delta * part) * speed
			invalidateMatrix()
		} else if (isCycle) {
			start = end.also { end = start }
			delta = end - start
		}
	}
}

fun View.rotateAnimation(byAngle: Angle, time: TimeSpan, isCycle: Boolean): View.(TimeSpan) -> Unit {
	var start = rotation
	var end = start + byAngle
	val dist = distance(end, start) as Angle
	var delta = end - start

	return { dt ->
		val part = dt / time

		val remainPart = (distance(rotation, end) as Angle) / dist
		if (remainPart > part) {
			rotation += (delta * part) * speed
		} else if (isCycle) {
			start = end.also { end = start }
			delta = end - start
		}
	}
}

fun View.scaleAnimation(destScale: Double, time: TimeSpan, isCycle: Boolean): View.(TimeSpan) -> Unit {
	var start = scale
	var end = destScale
	val dist = distance(end, start) as Double
	var delta = end - start

	return { dt ->
		val part = dt / time

		val remainPart = (distance(scale, destScale) as Double) / dist
		if (remainPart > part) {
			scale += (delta * part) * speed
			invalidateMatrix()
		} else if (isCycle) {
			start = end.also { end = start }
			delta = end - start
		}
	}
}

fun toStoppableUpdater(function: View.(TimeSpan) -> Unit): View.(TimeSpan) -> Unit = { dt ->
	if (isContinue) function(dt)
}

fun distance(a: Any, b: Any): Comparable<*> = when(a::class) {
	Point::class -> (a as Point).distanceTo(b as Point)
	Angle::class -> abs((b as Angle).radians - (a as Angle).radians).radians
	Double::class -> abs((b as Double) - (a as Double))
	else -> 0
}

// region Args parsing
val List<String>.type: String get() = get(0)
val List<String>.args: List<String> get() = slice(1 until size)

val List<String>.centerX: Double get() = get(0).toDouble()
val List<String>.centerY: Double get() = get(1).toDouble()
val List<String>.width: Double get() = get(2).toDouble()
val List<String>.height: Double get() = get(3).toDouble()
val List<String>.angle: Angle get() = get(4).toDouble().degrees
val List<String>.radius: Double get() = get(2).toDouble()
val List<String>.color: RGBA get() = Colors[last()]

val List<String>.destX: Double get() = get(0).toDouble()
val List<String>.destY: Double get() = get(1).toDouble()
val List<String>.rotateAngle: Angle get() = get(0).toDouble().degrees
val List<String>.destScale: Double get() = get(0).toDouble()
val List<String>.isCycle: Boolean get() = last() == "cycle"
val List<String>.time: TimeSpan get() = (if(isCycle) { get(lastIndex - 1) } else { last() }).toInt().milliseconds
// endregion
