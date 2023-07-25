package com.fedorov.andrii.andriiovych.qachallenge.presentation.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fedorov.andrii.andriiovych.qachallenge.domain.models.CategoryModel
import com.fedorov.andrii.andriiovych.qachallenge.domain.models.CheckAnswerParams
import com.fedorov.andrii.andriiovych.qachallenge.domain.models.QuestionModel
import com.fedorov.andrii.andriiovych.qachallenge.domain.models.QuestionParams
import com.fedorov.andrii.andriiovych.qachallenge.domain.repositories.ResultOfResponse
import com.fedorov.andrii.andriiovych.qachallenge.domain.usecases.CheckAnswerUseCase
import com.fedorov.andrii.andriiovych.qachallenge.domain.usecases.NewQuestionUseCase
import com.fedorov.andrii.andriiovych.qachallenge.ui.theme.ButtonBackgroundFalse
import com.fedorov.andrii.andriiovych.qachallenge.ui.theme.ButtonBackgroundTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseQuizViewModel (
    private val newQuestionUseCase: NewQuestionUseCase,
    private val checkAnswerUseCase: CheckAnswerUseCase,
) : ViewModel() {

    private val _screenState =
        MutableStateFlow<ScreenState<QuestionModel>>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState<QuestionModel>> = _screenState

    private var difficultyState = QuestionDifficulty.ANY

    var questionType = QuestionType.ANY

    var categoryState = CategoryModel()
        set(value) {
            field = value
            getNewQuestion()
        }

    fun getNewQuestion() = viewModelScope.launch {
        _screenState.value = ScreenState.Loading
        val result =
            newQuestionUseCase.getNewQuestion(
                QuestionParams(
                    category = categoryState.id,
                    type = questionType.value,
                    difficulty = difficultyState.value
                )
            )
        when (result) {
            is ResultOfResponse.Success<QuestionModel> -> _screenState.value =
                ScreenState.Success(value = result.value)
            is ResultOfResponse.Failure -> _screenState.value =
                ScreenState.Failure(message = result.message)
        }
    }

    fun checkCorrectAnswer(numberButton: Int) {
        val questionModel = (screenState.value as ScreenState.Success).value
        val result = checkAnswerUseCase.checkAnswers(
            CheckAnswerParams(
                answers = questionModel.answers,
                correctAnswer = questionModel.correct_answer,
                numberButton = numberButton
            )
        )
        val colorState = getColorStateForButton(numberButton)
        if (result) {
            correctAnswer(colorState)
        } else {
            wrongAnswer(colorState)
        }
    }

  abstract fun getColorStateForButton(numberButton: Int): MutableStateFlow<Color>

    private fun correctAnswer(colorState: MutableStateFlow<Color>) {
        colorState.value = ButtonBackgroundTrue
        updateQuestion()
    }

    private fun wrongAnswer(colorState: MutableStateFlow<Color>) {
        colorState.value = ButtonBackgroundFalse
    }

    private fun updateQuestion() = viewModelScope.launch {
        delay(500)
        resetButtonColor()
        getNewQuestion()
    }

    abstract fun resetButtonColor()
}