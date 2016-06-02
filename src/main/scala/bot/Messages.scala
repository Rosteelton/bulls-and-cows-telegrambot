package bot

/**
  * Created by ansolovev on 02.06.16.
  */
object Messages {

  val help = """
        Правила игры:
        Тебе нужно отгадать последовательность из 4 цифр.
        Цифры не могут повторяться.
        После твоего хода я скажу сколько получилось быков и коров.
        Бык (bull) - цифра присутствует и стоит именно на этом месте.
        Корова (cow) - цифра присутствует, но стоит не на своем месте.
        Набери /startnewgame, чтобы начать.
             """

  val startNewGame = """
          Game Started!
          Write "/step <1234>" for action
          write "/startnewgame" to restart your game!
                     """

}
