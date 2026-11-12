package io.github.steven_stu77.lazee.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons copied from the prototype's SVG paths (24×24, round stroke). */
object LazeeIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    private fun icon(name: String, width: Float = 1.8f, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Home = icon("home", 1.8f, "M3 10l9-7 9 7v10a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Stats = icon("stats", 1.8f, "M4 20V10M10 20V4M16 20v-7M22 20H2")
    val Plus = icon("plus", 2.4f, "M12 5v14M5 12h14")
    val Budget = icon("budget", 1.8f, circle(12f, 12f, 9f), "M12 3v9l6 4")
    val Me = icon("me", 1.8f, circle(12f, 8f, 4f), "M4 21c0-4 4-6 8-6s8 2 8 6")
    val Eye = icon("eye", 1.8f, "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", circle(12f, 12f, 3f))
    val EyeOff = icon("eyeOff", 1.8f, "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", "M3 3l18 18")
    val Bell = icon("bell", 1.8f, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val ChevronRight = icon("chevronRight", 2f, "M9 18l6-6-6-6")
    val ChevronLeft = icon("chevronLeft", 2f, "M15 18l-6-6 6-6")
    val ChevronDown = icon("chevronDown", 2f, "M6 9l6 6 6-6")
    val Calendar = icon("calendar", 1.8f, rect(3f, 4f, 18f, 17f, 2f), "M16 2v4M8 2v4M3 10h18")
    val Search = icon("search", 1.8f, circle(11f, 11f, 7f), "M21 21l-4.3-4.3")
    val Close = icon("close", 2f, "M18 6L6 18M6 6l12 12")
    val Camera = icon("camera", 1.8f, "M4 7h3l2-3h6l2 3h3a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z", circle(12f, 13f, 4f))
    val Check = icon("check", 2.2f, "M5 12l5 5L20 7")
    val Trash = icon("trash", 1.8f, "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14")
    val Edit = icon("edit", 1.8f, "M4 20h4L19 9l-4-4L4 16v4z")
    val Copy = icon("copy", 1.8f, rect(8f, 8f, 13f, 13f, 2f), "M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3")
    val Lock = icon("lock", 1.8f, rect(4f, 11f, 16f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")
    val Shield = icon("shield", 1.8f, "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z")
    val Undo = icon("undo", 1.8f, "M9 14L4 9l5-5", "M4 9h11a5 5 0 0 1 0 10h-3")
}
