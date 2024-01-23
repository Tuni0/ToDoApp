package com.example.todoapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.theme.ToDoAppTheme
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
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
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration


class SecondActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            val navController = rememberNavController()

            // Use NavHost and composable functions inside a @Composable function
            NavHost(navController = navController, startDestination = "mainScreen") {
                composable("mainScreen") {
                    // Pass the navController to the MainScreen composable
                    ToDoAppTheme {
                        TodoApp(navController)
                    }
                }
                composable("secondScreen") {
                    // Pass the navController to the SecondScreen composable
                    ToDoAppTheme {
                        SecondScreen(navController)
                    }
                }


            }
        }
    }
}

@SuppressLint("SuspiciousIndentation", "UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun SecondScreen(navController: NavController) {
    val (deletedTasks, setDeletedTasks) = remember { mutableStateOf(mutableListOf<Task>()) }


    val database = Firebase.database
    val deletedTasksReference = database.getReference("deletedTasks")

    var isPressed by remember { mutableStateOf(false) }

    // Function to fetch deleted tasks from Firebase and update the state
    fun fetchDeletedTasksFromFirebase() {
        deletedTasksReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedDeletedTasks = mutableListOf<Task>()

                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let { fetchedDeletedTasks.add(it) }
                }

                setDeletedTasks(fetchedDeletedTasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error reading deleted tasks from Firebase", error.toException())
            }
        })
    }
    // Use LaunchedEffect to fetch deleted tasks when the Composable is first launched
    LaunchedEffect(Unit) {
        fetchDeletedTasksFromFirebase()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF242424),
                    titleContentColor = Color(0xB3FFFFFF),
                ),
                title = {
                    Text("Top app bar")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFF242424),
                contentColor = Color(0xB3FFFFFF),
            ) {

                IconButton(modifier = Modifier
                    .height(50.dp)
                    .width(50.dp),
                    onClick = {
                        navController.popBackStack()

                    }) {

                    Icon(
                        imageVector = Icons.Outlined.SubdirectoryArrowLeft,
                        contentDescription = "Localized description" // You can set a content description if needed
                    )}
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // LazyColumn to display deleted tasks
            LazyColumn(
                modifier = Modifier
                    .background(Color.Transparent)
                    .animateContentSize(),

                ) {
                itemsIndexed(items = deletedTasks, itemContent = { index, item ->
                    val deletedTask = deletedTasks[index]
                    Row(
                        Modifier
                            .fillMaxWidth()

                    ) {

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

                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            isPressed = true
                                        },

                                        ) { isPressed = false }
                                },


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

                                    Checkbox(
                                        colors = CheckboxDefaults.colors(
                                            uncheckedColor = Color.Gray
                                        ),
                                        checked = true,
                                        onCheckedChange = { checked ->

                                        }


                                    )

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
                                        LocalDate.parse(deletedTask.date)
                                    )
                                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                                    val formattedTime = deletedTask.time.toString()

                                    Text(
                                        text=deletedTasks[index].name,
                                        textDecoration= TextDecoration.LineThrough,
                                        modifier=Modifier.fillMaxWidth(),
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
                                            text=" $formattedDate  $formattedTime",
                                            textDecoration= TextDecoration.LineThrough,
                                            modifier=Modifier.fillMaxWidth(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge

                                        )

                                    }

                                }
                            }
                        }
                    }

                })
            }
        }
    }
}
