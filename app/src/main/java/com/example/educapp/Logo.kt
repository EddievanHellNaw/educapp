import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@Composable
fun EduLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id=R.drawable.thinking_cap),
        contentDescription = "Educapp Logo",
        modifier = modifier)
    )
}