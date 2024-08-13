@Composable
fun LoginForm(modifier: Modifier = Modifier) {
    var username by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateof("")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            /*TODO*/
        }) {
            Text("Login")
        }
    }
}