package com.example.todoapp
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todoapp.ui.theme.ToDoAppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.Locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController

// inicjalizacja klasy głównej ekranu startowego
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            val navController = rememberNavController()


            NavHost(navController = navController, startDestination = "mainScreen") {
                composable("mainScreen") {
                    // nawigacja do widoku głównego ekranu
                    ToDoAppTheme { //motyw aplikacji opisany w ui.theme/Theme.kt
                        TodoApp(navController)
                    }
                }
                // nawigacja do widoku drugiego ekranu
                composable("secondScreen") {
                    ToDoAppTheme {
                        SecondScreen(navController)
                    }
                }
            }
        }
    }
}


    //klasa danych do przesłania na FIREBASE
    data class Task(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val date: String = "",
        val priority: String = "",
        val time: String = "",
        var checked: Boolean = false,

    ) {
        // zeroargumentowy konstruktor potrzebny do deserializacji danych dla firebase
        constructor() : this("", "", "", "", "","", false,)
    }

//funkcja dodawania zadań do firabasa
private fun addTaskToRealtimeDatabase(newTask: Task) {
    // inicjalizacja bazy danych i referencji do rodzica
    val database = Firebase.database
    val reference = database.getReference("tasks")

    // Use child(task.id) to set the child name to task.id
    val taskReference = reference.child(newTask.id)

    //mapowanie instancji dziecka, rodzica tasks w realtime database - "spawanie" parametrów  klasy danych z obiektami bazy danych
    val taskMap = mapOf(
        "id" to newTask.id,
        "name" to newTask.name,
        "description" to newTask.description,
        "date" to newTask.date,
        "priority" to newTask.priority,
        "time" to newTask.time,
        "checked" to newTask.checked,
    )

    //wpis do bazy danych
    taskReference.setValue(taskMap)
        .addOnSuccessListener {
            Log.d(TAG, "Task added to Realtime Database")
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error adding task to Realtime Database", e)
        }
}

//funkcja przemieszczająca wybrane zadania (dzieci) z rodzica tasks do deletedTasks, po wykonaniu zadania przez użytkownika
private fun moveCheckedTasksToDeletedParent(task: Task) {
    val database = Firebase.database
    val tasksReference = database.getReference("tasks")
    val deletedTasksReference = database.getReference("deletedTasks")

    val taskReference = tasksReference.child(task.id)

    val newDeletedTaskReference = deletedTasksReference.push() //przypisanie zadania do rodzica zadań wykonanych
    newDeletedTaskReference.setValue(task)

        .addOnSuccessListener {
            // po wykonanym przemieszczeniu dziecka usuń je od pierwotnego rodzica
            taskReference.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Task removed from tasks parent")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error removing task from tasks parent", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error moving task to deletedTasks", e)
        }
}


//funkcja główna
@SuppressLint("SuspiciousIndentation")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun TodoApp(navController: NavController) {

    // ustawienie kolorów specjalnych, elementów aplikacji
    val customCardColors = CardDefaults.cardColors(
        contentColor = MaterialTheme.colorScheme.secondary,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        disabledContentColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.onSurface,
    )
    // ustawienia kart
    val customCardElevation = CardDefaults.cardElevation(
        defaultElevation = 8.dp,
        pressedElevation = 2.dp,
        focusedElevation = 4.dp
    )

    val (tasks, setTasks) = remember { mutableStateOf(mutableListOf<Task>()) } //zadania,ustaw zadania (get,set)


    val database = Firebase.database
    val reference = database.getReference("tasks")

    // pobierz zadania z firebasa
    fun fetchTasksFromFirebase() {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedTasks = mutableListOf<Task>()

                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let { fetchedTasks.add(it) }
                }

                setTasks(fetchedTasks) //wgraj zadania
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error reading tasks from Firebase", error.toException())
            }
            private fun updateTaskCheckedStatus(taskId: String, isChecked: Boolean) {
                val taskReference = reference.child(taskId)
                taskReference.child("checked").setValue(isChecked)
                taskReference.child("deleted").setValue(isChecked)
            }
        })
    }

    // wgraj zadania przy włączeniu aplikacji
    LaunchedEffect(Unit) {
        fetchTasksFromFirebase()
    }

    //inicjalizacja zmiennych do klasy danych Task
    val taskName = remember { mutableStateOf("") }
    val taskDescription = remember { mutableStateOf("") }
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }



    var isPrioritySelected by remember { mutableStateOf(false) }
    var sentButtonPress by remember { mutableStateOf(false) }
    var calendarButtonPress by remember { mutableStateOf(false) }
    var priorityButtonPress by remember { mutableStateOf(false) }
    var floatingButtonPress by remember { mutableStateOf(false) }
    var timeButtonPress by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = FocusRequester()

    val datePickerState = rememberDatePickerState()
    var selectedPriority by remember { mutableStateOf("Low") }

    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now()
    val selectedTime = remember { mutableStateOf(LocalTime.now()) }

    val deletedItem = remember { mutableStateListOf<Task>() }


    var isPressed by remember { mutableStateOf(false) }


    //typ ui ekranu głównego
    Scaffold(
        containerColor = Color.Black,

        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = Color(0xFF242424),
                    titleContentColor = Color(0xB3FFFFFF),
                ),
                title = {
                    Text("Wiktor Mazepa Menedżer Zadań")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = Color(0xFF242424),
                contentColor = Color(0xB3FFFFFF),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon on the left side
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(vertical=60.dp, horizontal = 35.dp)
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onTertiary)
                            .clickable {
                                navController.navigate("mainScreen")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home Icon",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp)) // Add some space between icons

                    // Icon on the right side
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(vertical=60.dp, horizontal = 35.dp)
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onTertiary)
                            .clickable {
                                navController.navigate("secondScreen")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestoreFromTrash,
                            contentDescription = "Restore Icon",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    floatingButtonPress = true
                    keyboardController?.show()

                },

                ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }


    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


                LazyColumn(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .animateContentSize(),

                    ) {

                    itemsIndexed(items = tasks, itemContent = { index, item ->

                        val task = tasks[index]

                        //animacja usuwania zadań
                        AnimatedVisibility(
                            visible = !deletedItem.contains(item),
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { -it }),
                        ) {

                            Row(
                                Modifier
                                    .fillMaxWidth()

                            ) {

                                //główny widok karty utworzonego zadania
                                Card(

                                    shape = RectangleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPressed) Color.DarkGray else Color.Black,
                                        disabledContainerColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth() // Make the Card full width
                                        .height(120.dp)
                                        .padding(0.dp)
                                        .clickable(
                                            enabled = true,
                                            onClick = {},

                                            )
                                        .animateEnterExit(
                                            exit = slideOutVertically()
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    isPressed = true
                                                },

                                                ) { isPressed = false }
                                        },
                                    elevation = customCardElevation


                                ) {
                                    Divider(
                                        color = Color.Gray,
                                        thickness = 1.dp,
                                        modifier = Modifier.fillMaxWidth(1f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(0.3f)
                                                .fillMaxHeight(),
                                            verticalArrangement = Arrangement.Center


                                        ) {

                                            //ustawienie odpowiedniego koloru względem priorytetu zadania -- jeszcze w fazie ewaluacji
                                            when (selectedPriority) {
                                                "Low" -> {
                                                    Checkbox(
                                                        colors = CheckboxDefaults.colors(
                                                            uncheckedColor = Color.Gray
                                                        ),
                                                        checked = task.checked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {

                                                                moveCheckedTasksToDeletedParent(task)

                                                                // usuń zadanie po zaznaczeniu checkboxa
                                                                val updatedTasks = tasks.toMutableList().apply {
                                                                    remove(task)
                                                                }
                                                                setTasks(updatedTasks)
                                                                deletedItem.add(task)
                                                            } else {
                                                            }

                                                        }
                                                    )

                                                }

                                                "Medium" -> {
                                                    Checkbox(
                                                        colors = CheckboxDefaults.colors(
                                                            uncheckedColor = Color.Yellow
                                                        ),
                                                        checked = task.checked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {

                                                                moveCheckedTasksToDeletedParent(task)

                                                                val updatedTasks = tasks.toMutableList().apply {
                                                                    remove(task)
                                                                }
                                                                setTasks(updatedTasks)
                                                                deletedItem.add(task)
                                                            } else {
                                                            }
                                                        }
                                                    )

                                                }

                                                else -> {
                                                    Checkbox(
                                                        colors = CheckboxDefaults.colors(
                                                            uncheckedColor = Color.Red
                                                        ),
                                                        checked = task.checked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {

                                                                moveCheckedTasksToDeletedParent(task)

                                                                val updatedTasks = tasks.toMutableList().apply {
                                                                    remove(task)
                                                                }
                                                                setTasks(updatedTasks)
                                                                deletedItem.add(task)
                                                            } else {
                                                            }


                                                        }
                                                    )

                                                }
                                            }
                                        }


                                        Column(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(2f),
                                            verticalArrangement = Arrangement.Center
                                        )
                                        {

                                            val dateFormatter = DateTimeFormatter.ofPattern(
                                                "dd MMMM",
                                                Locale("pl")
                                            )
                                            val formattedDate = dateFormatter.format(
                                                LocalDate.parse(task.date)
                                            )


                                            Text(
                                                tasks[index].name,
                                                Modifier.fillMaxWidth(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                            Row(
                                                verticalAlignment = CenterVertically,
                                                modifier = Modifier.padding(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    tint = Color.White,
                                                    contentDescription = "Calendar Icon",
                                                    modifier = Modifier.size(14.dp)
                                                )

                                                Text(
                                                    //jeśli format czasu posiada sekundy i milisekundy sformatuj odpowiednio, jeśli nie wpisz stringa od razu (problem z niewybraniem godziny zadania)
                                                    text = if (task.time.length >= 7) {
                                                        " $formattedDate " + LocalTime.parse(task.time,DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS")).format(DateTimeFormatter.ofPattern("HH:mm"))
                                                    } else {
                                                        " $formattedDate " + task.time
                                                    },
                                                    modifier=Modifier.fillMaxWidth(),
                                                    color = Color.LightGray,
                                                    style = MaterialTheme.typography.bodyLarge

                                                )

                                            }

                                        }
                                    }
                                }
                            }

                    }
                })


            }

        }
    }


    // przycisk do wywołania tworzenia zadania
    if (floatingButtonPress) {

        //pokaż/schowaj klawiature
        DisposableEffect(Unit) {
            if (floatingButtonPress) {
                keyboardController?.show()
                focusRequester.requestFocus()
            }
            onDispose {
                if (floatingButtonPress) {
                    keyboardController?.hide()
                    floatingButtonPress = false
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth() // Make the Box full width
                .fillMaxHeight()


        ) {

            //karta dodawania zadań
            Card(
                colors = customCardColors,
                elevation = customCardElevation,
                modifier = Modifier
                    .fillMaxWidth() // Make the Card full width
                    .height(250.dp)
                    .padding(0.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        TextField(
                            value = text1,
                            onValueChange = { text1 = it },
                            placeholder = {
                                Text(
                                    "Task Name",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType=KeyboardType.Text),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    floatingButtonPress = false
                                }
                            ),

                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),

                            textStyle = TextStyle(
                                fontSize = 12.sp,

                                )


                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        TextField(
                            value = text2,
                            onValueChange = { text2 = it },
                            placeholder = {
                                Text(
                                    "Description",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    floatingButtonPress = false
                                }

                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSecondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ), //hide the indicator
                            textStyle = TextStyle(
                                fontSize = 16.sp,


                                )


                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(modifier = Modifier
                            .height(50.dp)
                            .width(50.dp),
                            onClick = {
                                calendarButtonPress = true

                            }) {

                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Localized description" // You can set a content description if needed
                            )

                        }


                        IconButton(
                            modifier = Modifier
                                .height(50.dp)
                                .width(50.dp),
                            onClick = {
                                priorityButtonPress = true

                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Localized description", // You can set a content description if needed

                            )

                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(modifier = Modifier
                            .height(50.dp)
                            .width(50.dp),

                            onClick = {
                                taskName.value = text1
                                taskDescription.value = text2

                                val newTask = Task(
                                    id = UUID.randomUUID().toString(),
                                    name = taskName.value,
                                    description = taskDescription.value,
                                    date = selectedDate.toString(),
                                    priority = selectedPriority,
                                    time = selectedTime.value.toString(),
                                    checked = false,
                                )

                                // dodaj nowo utworzone zadanie do bazy danych
                                addTaskToRealtimeDatabase(newTask)



                                // ukryj klawiature i przywróc odpowiednie wartości guzików
                                keyboardController?.hide()
                                sentButtonPress = true
                                floatingButtonPress = false

                                // zresetuj flagi --- jeszcze w fazie ewaluacji
                                isPrioritySelected = false
                                selectedPriority = "Low"

                            })
                        {

                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Localized description"
                            )

                        }


                    }
                }
            }
        }

    }



    // funkcje wybierania godziny i daty
    @ExperimentalMaterial3Api
    @Composable
    fun SheetPickerDialog(

        title: String = "Select Time",
        onCancel: () -> Unit,
        onConfirm: () -> Unit,
        toggle: @Composable () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        Dialog(
            onDismissRequest = onCancel,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min)
                    .background(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface
                    ),
            ) {
                toggle()
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        text = title,
                        style = MaterialTheme.typography.labelMedium
                    )
                    content()
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onCancel
                        ) { Text("Cancel") }
                        TextButton(
                            onClick = onConfirm
                        ) { Text("OK") }
                    }
                }
            }
        }
    }

    @Composable
    fun BottomSheetDatePicker(
        state: DatePickerState,
        calendarButtonPress: SheetState = rememberModalBottomSheetState(),
        onDismissRequest: () -> Unit,
        selectedTime: MutableState<LocalTime>
    ) {

        ModalBottomSheet(
            modifier = Modifier.requiredHeight(1300.dp),
            onDismissRequest = onDismissRequest,
            sheetState = calendarButtonPress,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {

            //wygląd wybierania daty
            DatePicker(
                state = state,
                showModeToggle = false,
                title = null,
                headline = null
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(modifier = Modifier
                    .height(50.dp)
                    .width(50.dp),
                    onClick = {
                        timeButtonPress = true

                    }) {

                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "Localized description"
                    )


                }
                Spacer(modifier = Modifier.weight(0.5f))

                // wyświetl wybraną godzinę w polu tekstowym
                TextField(
                    value = selectedTime.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onValueChange = {}, // Disable text input
                    readOnly = true, // Make the TextField read-only
                    modifier = Modifier.padding(16.dp)
                )
                if (timeButtonPress) {
                    val initialHour = selectedTime.value.hour
                    val initialMinute = selectedTime.value.minute
                    val is24Hour = true
                    val timePickerState = remember {
                        mutableStateOf(
                            TimePickerState(
                                initialHour,
                                initialMinute,
                                is24Hour
                            )
                        )
                    }

                    SheetPickerDialog(
                        onCancel = { timeButtonPress = false },
                        onConfirm = {
                            timeButtonPress = false
                            selectedTime.value = LocalTime.of(
                                timePickerState.value.hour,
                                timePickerState.value.minute
                            )

                        },
                    ) {
                        TimePicker(state = timePickerState.value)


                    }
                }
            }
        }

    }



    //jeszcze nie działa - obsługa priorytetu
    @Composable
    fun PriorityItem(
        priority: String,
        onPrioritySelected: (String) -> Unit
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    onPrioritySelected(priority)
                }
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
                .height(50.dp)
                .width(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = priority,
                color = Color.White
            )
        }
    }

    @Composable
    fun PriorityBottomSheet(
        priorityButtonPress: Boolean,
        onDismissRequest: () -> Unit,
        onPrioritySelected: (String) -> Unit
    ) {
        if (priorityButtonPress) {
            ModalBottomSheet(
                modifier = Modifier.requiredHeight(1300.dp),
                onDismissRequest = { onDismissRequest() },
                sheetState = rememberModalBottomSheetState(),
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                // Your UI for displaying priority options
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                ) {
                    val priorities = listOf("Low", "Medium", "High")
                    items(priorities) { priority ->
                        PriorityItem(
                            priority = priority,
                            onPrioritySelected = {
                                onPrioritySelected(it)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }

    //ikona przycisku do wybierania daty
    if (calendarButtonPress) {
        BottomSheetDatePicker(
            state = datePickerState,
            onDismissRequest = { calendarButtonPress = false },
            selectedTime = selectedTime
        )
    }


    if (priorityButtonPress) {
        PriorityBottomSheet(
            priorityButtonPress = priorityButtonPress,
            onDismissRequest = { priorityButtonPress = false },
            onPrioritySelected = { newPriority ->
                selectedPriority = newPriority
            }
        )
    }
}
