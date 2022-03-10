package pt.isel.leic.pc.imageviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import pt.isel.leic.pc.imageviewer.filters.adjustBrightnessST
import pt.isel.leic.pc.imageviewer.filters.convertToGrayScaleST
import java.io.File

/**
 * Composable that defines the application's main window.
 * @param onCloseRequested  The function to be called when the user intends to close the window
 */
@Composable
fun MainWindow(onCloseRequested: () -> Unit) = Window(onCloseRequest = onCloseRequested, title = "Image Viewer") {

    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    val isGrayScale = remember { mutableStateOf(false) }
    val adjustBrightness = remember { mutableStateOf(false) }

    fun loadImage(file: File) {
        file.inputStream().use {
            // NOTE: This is wrong! It MUST be done without blocking the calling thread, because it is called from
            // an event handler. Because all events are delivered sequentially, if we block the calling thread, the
            // handling of all subsequent events is delayed and therefore the user experience suffers.
            print("Loading image ... ")
            imageBitmap.value = loadImageBitmap(it)
            println("done!")
        }
    }

    MaterialTheme {
        MainWindowMenu(
            onQuit = onCloseRequested,
            onLoad = { openImageFilePicker(onImageFilePicked = { if (it != null) loadImage(it) }) },
            isGrayScaleEnabled = isGrayScale.value,
            onGrayScaleChanged = { isGrayScale.value = it },
            isBrightnessEnabled = adjustBrightness.value,
            onBrightnessChanged = { adjustBrightness.value = it },
        )

        MainWindowContent(
            imageBitmap = imageBitmap.value,
            convertToGrayscale = isGrayScale.value,
            adjustBrightness = adjustBrightness.value
        )
    }
}

/**
 * The application's main window menu
 */
@Composable
fun FrameWindowScope.MainWindowMenu(
    onLoad: () -> Unit,
    onQuit: () -> Unit,
    isGrayScaleEnabled: Boolean,
    onGrayScaleChanged: (Boolean) -> Unit,
    isBrightnessEnabled: Boolean,
    onBrightnessChanged: (Boolean) -> Unit
) = MenuBar {
    Menu("File") {
        Item(text = "Open", onClick = onLoad)
        Separator()
        Item(text = "Quit", onClick = onQuit)
    }
    Menu("Image") {
        CheckboxItem(text = "Grayscale", checked = isGrayScaleEnabled, onCheckedChange = onGrayScaleChanged)
        CheckboxItem(text = "Brightness", checked = isBrightnessEnabled, onCheckedChange = onBrightnessChanged)
    }
}

@Composable
fun MainWindowContent(
    imageBitmap: ImageBitmap?,
    convertToGrayscale: Boolean,
    adjustBrightness: Boolean
) = Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

    // IMPORTANT NOTE:
    // Intensive compute bound work should be off-loaded to other threads. In this case, we are delaying
    // composition, which can happen many times. This is true for both single threaded and multithreaded
    // versions (ST and MT). We will fix this before the semester ends! =)

    println("Composing MainWindowContent with non-null imageBitmap? ${imageBitmap != null}")
    val rememberKey = Pair(imageBitmap, convertToGrayscale)
    val currentImage = remember(rememberKey) {
        println("Running block to remember with key = $rememberKey")
        imageBitmap?.let {
            if (convertToGrayscale) convertToGrayScaleST(imageBitmap) else imageBitmap
        }
    }

    val brightness = remember { mutableStateOf(Pair(0.0f, false)) }
    if (currentImage != null) {

        val (current, isFinalValue) = brightness.value
        val imageToDisplay = if (isFinalValue) adjustBrightnessST(currentImage, current) else currentImage
        println("Composition of Image starts")
        Image(imageToDisplay, "", modifier = Modifier.weight(1.0f).align(Alignment.CenterHorizontally))
        println("Composition of Image ends")

        if (adjustBrightness) {
            Row(modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp)) {
                Text("${(current * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.width(32.dp))
                Slider(
                    value = current,
                    valueRange = 0.0f..0.5f,
                    modifier = Modifier.weight(0.1f),
                    onValueChange = { brightness.value = Pair(it, false) },
                    onValueChangeFinished = { brightness.value = Pair(current, true) }
                )
            }
        }
    }
    println("Composition of MainWindowContent ends")
}