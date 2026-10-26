package de.gamescollection.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small app-specific vectors avoid bundling the entire extended icon catalog. */
internal object NativeIcons {
    val Discover = icon("Discover", "M12,2a10,10 0,1 0,0 20a10,10 0,1 0,0 -20zM12,4a8,8 0,1 1,0 16a8,8 0,1 1,0 -16zM16,8l-3,5l-5,3l3,-5z")
    val Library = icon("Library", "M3,3h8v8H3zM13,3h8v8h-8zM3,13h8v8H3zM13,13h8v8h-8z")
    val Statistics = icon("Statistics", "M4,13h4v7H4zM10,4h4v16h-4zM16,9h4v11h-4z")
    val Filters = icon("Filters", "M3,5h10v2H3zM17,5h4v2h-4zM13,3h4v6h-4zM3,11h3v2H3zM10,11h11v2H10zM6,9h4v6H6zM3,17h10v2H3zM17,17h4v2h-4zM13,15h4v6h-4z")
    val Gamepad = icon("Gamepad", "M6,5h12a4,4 0,0 1,4 4v8a3,3 0,0 1,-5 2l-3,-3h-4l-3,3a3,3 0,0 1,-5 -2v-8a4,4 0,0 1,4 -4zM6,8h2v2h2v2h-2v2h-2v-2H4v-2h2zM15,8h2v2h-2zM18,11h2v2h-2z")

    private fun icon(name: String, data: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(data).toNodes(), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd).build()
}
