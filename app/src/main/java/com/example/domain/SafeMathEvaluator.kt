package com.example.domain

/**
 * Dedicated tokenizer and recursive-descent arithmetic evaluator supporting +, -, *, /,
 * unary +/-, decimals, and parentheses. Never uses eval() or scripting engines.
 */
object SafeMathEvaluator {

    private sealed interface Token {
        data class Number(val value: Double) : Token
        data class Operator(val symbol: Char) : Token
        data object LeftParen : Token
        data object RightParen : Token
    }

    fun evaluate(rawInput: String): Result<Double> {
        val cleaned = rawInput
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .trim()

        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter an amount"))
        }

        val tokens = tokenize(cleaned).getOrElse { return Result.failure(it) }
        if (tokens.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter an amount"))
        }

        val parser = Parser(tokens)
        return try {
            val value = parser.parseExpression()
            if (!parser.isAtEnd()) {
                Result.failure(IllegalArgumentException("Unexpected symbol in expression"))
            } else if (value.isNaN() || value.isInfinite()) {
                Result.failure(IllegalArgumentException("Invalid math result"))
            } else {
                Result.success(FinanceMath.roundCurrency(value))
            }
        } catch (e: ArithmeticException) {
            Result.failure(IllegalArgumentException(e.message ?: "Division by zero"))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    private fun tokenize(input: String): Result<List<Token>> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotCount = 0
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i] == '.') dotCount++
                        i++
                    }
                    if (dotCount > 1) {
                        return Result.failure(IllegalArgumentException("Invalid decimal number"))
                    }
                    val numStr = input.substring(start, i)
                    val parsed = numStr.toDoubleOrNull()
                        ?: return Result.failure(IllegalArgumentException("Invalid number '$numStr'"))
                    tokens.add(Token.Number(parsed))
                }
                ch == '+' || ch == '-' || ch == '*' || ch == '/' -> {
                    tokens.add(Token.Operator(ch))
                    i++
                }
                ch == '(' -> {
                    tokens.add(Token.LeftParen)
                    i++
                }
                ch == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }
                else -> {
                    return Result.failure(IllegalArgumentException("Unsupported character '$ch'"))
                }
            }
        }
        return Result.success(tokens)
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        // expression := term (('+' | '-') term)*
        fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.symbol == '+' || next.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (next.symbol == '+') left + right else left - right
                } else {
                    break
                }
            }
            return left
        }

        // term := unary (('*' | '/') unary)*
        private fun parseTerm(): Double {
            var left = parseUnary()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.symbol == '*' || next.symbol == '/')) {
                    consume()
                    val right = parseUnary()
                    if (next.symbol == '/') {
                        if (right == 0.0) throw ArithmeticException("Cannot divide by zero")
                        left /= right
                    } else {
                        left *= right
                    }
                } else {
                    break
                }
            }
            return left
        }

        // unary := ('+' | '-') unary | primary
        private fun parseUnary(): Double {
            val next = peek()
            if (next is Token.Operator && (next.symbol == '+' || next.symbol == '-')) {
                consume()
                val operand = parseUnary()
                return if (next.symbol == '-') -operand else operand
            }
            return parsePrimary()
        }

        // primary := NUMBER | '(' expression ')'
        private fun parsePrimary(): Double {
            return when (val next = peek()) {
                is Token.Number -> {
                    consume()
                    next.value
                }
                is Token.LeftParen -> {
                    consume()
                    val inner = parseExpression()
                    if (peek() !is Token.RightParen) {
                        throw IllegalArgumentException("Missing closing parenthesis")
                    }
                    consume()
                    inner
                }
                else -> throw IllegalArgumentException("Incomplete expression")
            }
        }
    }
}
