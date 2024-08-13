import Logo.kt
import LoginForm.kt

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
@Composable
fun MainScreen() {
    var showLoginForm by remember {
        mutableStateOf(false)
    }
    var animationFinished by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        delay(1000)
        animationFinished = true
        delay(500)
        showLoginForm = true
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EduLogo(
            modifier = Modifier
                .size(120.dp)
                .alpha(if (animationFinished) 1f else 0f)
                .scale(if (animationFinished) 1f else 0.5f)
                .animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        if (showLoginForm) {
            LoginForm(modifier = Modifier.padding(16.dp))
        }
    }
}