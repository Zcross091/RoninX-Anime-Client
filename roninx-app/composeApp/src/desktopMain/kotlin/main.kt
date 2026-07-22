import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "RoninX") {
        App()
    }
}
