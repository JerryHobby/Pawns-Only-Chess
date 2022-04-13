package chess

import java.lang.Math.abs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// This version has enhancements.  It adds a "history" command than can print
// the game in PGN format.  It also adds a "help" command.
// The validateMove function is a bit long.  There's some cleanup to do on how
// board coordinates are stored and used.  But overall it's a good version.
//
// I also created a "test" command that enables move "test" command that
// makes testing much easier because moves do not actually get made.  That's
// also how I validate the "no more moves" condition.

class ChessBoard {
    private val boardSize: Int = 8

    class Square {
        var piece: String = " "
    }

    // define the double array that holds all the squares.  A few variations.
    // val board = MutableList(boardSize) { MutableList(boardSize) { Square() } }
    // val board = Array(boardSize) { row -> Array(boardSize) { col -> Square() } }
    private val board = Array(boardSize) { Array(boardSize) { Square() } }

    init {
        resetBoard()
    }

    fun resetBoard() {
        // setup the board to start a new game
        for (x in 0 until boardSize) {
            board[6][x].piece = "B"
            board[1][x].piece = "W"
        }
    }

    fun movePiece(player: Player, fromX: Int, fromY: Int, toX: Int, toY: Int): Int {
        if ((fromX < 0 || fromX >= boardSize) || (fromY < 0 || fromY >= boardSize) || (toX < 0 || toX >= boardSize) || (toY < 0 || toY >= boardSize)) return 0

        board[toX][toY].piece = board[fromX][fromY].piece
        board[fromX][fromY].piece = " "
        return 1
    }

    fun movePiece(player: Player, fromCoord: String, toCoord: String): Int {
        if (fromCoord.length != 2 || toCoord.length != 2 || fromCoord == toCoord) return 0

        val fromXY: List<Int> = convertCoords(fromCoord)
        val toXY: List<Int> = convertCoords(toCoord)

        return movePiece(player, fromXY[0], fromXY[1], toXY[0], toXY[1])
    }

    fun movePiece(player: Player, coord: String): Int {
        if (coord.length != 4) return 0

        return movePiece(player, coord.substring(0, 2), coord.substring(2, 4))
    }

    fun setPiece(coord: String, value: String = " ") {
        if (coord.length != 2) return

        val fromXY: List<Int> = convertCoords(coord)
        board[fromXY[0]][fromXY[1]].piece = value
    }

    fun getPiece(coord: String): String {
        if (coord.length != 2) return ""

        val fromXY: List<Int> = convertCoords(coord)
        return board[fromXY[0]][fromXY[1]].piece
    }

    private fun convertCoords(position: String): List<Int> {
        if (position[0].isLetter() == false || position[1].isDigit() == false) return listOf(-1, -1) // trigger an error

        return listOf(position[1].digitToInt() - 1, position.uppercase()[0] - 'A')
    }

    fun print() {
        println("  +---+---+---+---+---+---+---+---+")

        for (x in board.size - 1 downTo 0) {
            print("${x + 1} ")
            for (y in 0 until board[x].size) print("| ${board[x][y].piece} ")
            println("|")
            println("  +---+---+---+---+---+---+---+---+")
        }
        println("    a   b   c   d   e   f   g   h ")
    }
}

class History {
    //    private val board = Array(boardSize) { Array(boardSize) { Square() } }
    // var history = mutableListOf(mutableListOf(2, { String() }))
    var history = mutableListOf<List<String>>()

    fun add(from: String, to: String, action: String = " ") {
        history.add(listOf(from, to, action))
    }

    fun print() {
        for (x in 0 until history.size) print("${x + 1}. ${history[x][0]}${history[x][2]}${history[x][1]} ")
        println()
    }

    fun lastMove(): List<String>? {
        if (history.size == 0) return null

        return history.last()
    }
}

class Player {
    var name: String = ""
    var color: String = " "
    var score: Int = 8
    val homeX: Int
        get () {
            if (color == "W")
                return 2
            if (color == "B")
                return 7
            return 0
        }
    val direction: Int
        get () {
            if (color == "W")
                return 1
            if (color == "B")
                return -1
            return 0
        }
}

fun validMove(board: ChessBoard,
              player: Player,
              opponent: Player,
              move: String,
              history: History,
              makeMove: Boolean = true): String {

    // validate syntax
    if (move.matches(("[a-hA-H][1-8][a-hA-H][1-8]").toRegex()) == false) {
        return "Invalid Input"
    }

    val fromY: Char = move[0]
    val fromX: Int = move[1].digitToInt()
    val toY: Char = move[2]
    val toX: Int = move[3].digitToInt()
    val fromPiece: String = board.getPiece(move.substring(0, 2))
    val toPiece: String = board.getPiece(move.substring(2, 4))
    val skipPiece: String = board.getPiece("%s%d".format(fromY, player.homeX + (1 * player.direction)))
    var action: String = " "  // used for history

    // verify "from" piece is there and correct piece
    if (fromPiece != player.color) {
        if (player.color == "W") return "No white pawn at ${move.substring(0, 2)}"
        else return "No black pawn at ${move.substring(0, 2)}"
    }

    if ((toX - fromX) * player.direction < 1) {
        // no lateral or negative moves
        return "Invalid Input"
    }

    // rules
    // if forward move?
    if (toY == fromY) {
        // moving forward
        if (toX == fromX) {
            // no move
            return "Invalid Input"
        }
        // verify single space move (up or down direction)
        if ((toX - fromX) * player.direction < 1) {
            return "Invalid Input"
        }
        // verify target is empty

        if (toPiece != " ") {
            // occupied
            return "Invalid Input"
        }
        // verify double space move
        if ((toX - fromX) * player.direction == (2)) {
            // verify target is empty
            if (skipPiece != " " || fromX != player.homeX) {
                // verify at home and the extra space we skipped is empty
                return "Invalid Input"
            }
        }
        if ((toX - fromX) * player.direction > 2) {
            // too far
            return "Invalid Input"
        }

        if(makeMove) {
            // make the move - one or two spaces as requested
            if (board.movePiece(player, move) == 0) {
                return "Invalid Input"
            }
            // forward move verified
            history.add(move.substring(0, 2), move.substring(2, 4), action)
        }
        return "OK"
    }

    // if capture/diagonal move ---
    // do not move to diagonal unless occupied by opponent, action = "x"
    if (fromY - toY > 1 || fromY - toY < -1) {
        // diagonal move by too many columns
        return "Invalid Input"
    }
    if ((toX - fromX) * player.direction > 1) {
        // diagonal move by too many rows
        return "Invalid Input"
    }

    if (toPiece == opponent.color) {
        // normal capture
        action = "x"
        // make the move - one or two spaces as requested

        if(makeMove) {
            if (board.movePiece(player, move) == 0) {
                return "Invalid Input"
            }
            // successful
            history.add(move.substring(0, 2), move.substring(2, 4), action)
            opponent.score -= 1
        }
        return "OK"
    }

    if (toPiece == " ") {
        // verify en passant
        // was last move a 2-space move
        // set opponent to empty
        // make move
        //val toY: Char = move[2]
        //val toX: Int = move[3].digitToInt()

        val lastMove = history.lastMove()
        if (lastMove == null) {
            return "Invalid Input"
        }

        val distanceX = abs(lastMove[0][0] - lastMove[1][0])
        val distanceY = abs(lastMove[0][1].digitToInt() - lastMove[1][1].digitToInt())

        if (distanceX != 0 || distanceY != 2) {
            // diagonal move by too many rows
            return "Invalid Input"
        }

        // if last move was same column as our target
        if (lastMove[0][0] != toY) {
            // diagonal move by too many rows
            return "Invalid Input"
        }

        action = "x"
        // make the move - one or two spaces as requested

        // successful
        if(makeMove) {
            if (board.movePiece(player, move) == 0) {
                return "Invalid Input"
            }
            board.setPiece(lastMove[1], " ")
            history.add(move.substring(0, 2), move.substring(2, 4), action)
            opponent.score -= 1
        }
        return "OK"
    }
    // should never reach here
    return "Invalid Input"
}

fun main() {
    val board = ChessBoard()
    val player = Array(2) { Player() }
    val history = History()
    var makeMove: Boolean = true

    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val date = current.format(formatter)

    println("Pawns-Only Chess")

    // setup the players
    print("First Player's name: ")
    player[0].name = readln()
    if (player[0].name == "") player[0].name = "Player 1"
    player[0].color = "W"

    print("Second Player's name: ")
    player[1].name = readln()
    if (player[1].name == "") player[1].name = "Player 2"
    player[1].color = "B"

    board.print()

    // setup the game loop
    var command: String = ""
    var playersTurn: Int = 0
    var opponentsTurn: Int = 1

    while (command != "exit") {

        print("${player[playersTurn].name}'s turn: ")

        command = readln()

        if (command == "exit") continue

        if (command == "print") {
            board.print()
            continue
        }
        if (command == "history") {
            println("[Event \"Pawns-Only Chess\"]")
            println("[Date \"$date\"]")
            println("[White \"${player[0].name}\"]")
            println("[Black \"${player[1].name}\"]")
            history.print()
            continue
        }

        if (command == "help" || command == "?") {
            println("Commands: help, history, last, print, score, test, exit.")
            println("Move format: [a-h][1-8][a-h][1-8]")
            continue
        }

        if (command == "score") {
            println("[${player[0].color}] ${player[0].name}: ${player[0].score}")
            println("[${player[1].color}] ${player[1].name}: ${player[1].score}")
            continue
        }
        if (command == "last") {
            println(history.lastMove())
            continue
        }

        if (command == "test") {
            if(makeMove) {
                println("Testing mode")
                makeMove = false
            } else {
                println("Play mode")
                makeMove = true
            }
            continue
        }

        var rcMsg: String
        rcMsg = validMove( board, player[playersTurn], player[opponentsTurn],
            command, history, makeMove)

        if (makeMove == false) {
            println("Test: ${rcMsg}")
            continue
        }

        if (rcMsg != "OK") {
            println(rcMsg)
            continue
        }

        // check win condition
        //The program should check whether the winning or stalemate
        // conditions are met after each turn.
        // If the winning conditions are fulfilled, print White Wins!
        // or Black Wins!, depending on the situation.
        // Then, print Bye! and exit the program.
        // If it's a stalemate, print Stalemate!, Bye! and exit the program.


        board.print()
        //toggle active player
        playersTurn = playersTurn xor 1
        opponentsTurn = opponentsTurn xor 1
    }
    println("Bye!")
}