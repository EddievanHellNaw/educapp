@Composable
fun EduLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id=R.drawable.thinking_cap),
        contentDescription = "Educapp Logo",
        modifier = modifier)
    )
}