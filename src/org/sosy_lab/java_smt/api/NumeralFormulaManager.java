// This file is part of JavaSMT,
// an API wrapper for a collection of SMT solvers:
// https://github.com/sosy-lab/java-smt
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.java_smt.api;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * This interface represents the Numeral Theory.
 *
 * @param <ParamFormulaType> formulaType of the parameters
 * @param <ResultFormulaType> formulaType of arithmetic results
 */
public interface NumeralFormulaManager<
    ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula> {

  ResultFormulaType makeNumber(long number);

  ResultFormulaType makeNumber(BigInteger number);

  /**
   * Create a numeric literal with a given value. Note: if the theory represented by this instance
   * cannot handle rational numbers, the value may get rounded or otherwise represented imprecisely.
   */
  ResultFormulaType makeNumber(double number);

  /**
   * Create a numeric literal with a given value. Note: if the theory represented by this instance
   * cannot handle rational numbers, the value may get rounded or otherwise represented imprecisely.
   */
  ResultFormulaType makeNumber(BigDecimal number);

  ResultFormulaType makeNumber(String pI);

  ResultFormulaType makeNumber(Rational pRational);

  /**
   * Creates a variable with exactly the given name.
   *
   * <p>Please make sure that the given name is valid in SMT-LIB2. Take a look at {@link
   * FormulaManager#isValidName} for further information.
   *
   * <p>This method does not quote or unquote the given name, but uses the plain name "AS IS".
   * {@link Formula#toString} can return a different String than the given one.
   */
  ResultFormulaType makeVariable(String pVar);

  FormulaType<ResultFormulaType> getFormulaType();

  // ----------------- Arithmetic relations, return type NumeralFormula -----------------

  ResultFormulaType negate(ParamFormulaType number);

  ResultFormulaType add(ParamFormulaType number1, ParamFormulaType number2);

  ResultFormulaType sum(List<ParamFormulaType> operands);

  ResultFormulaType subtract(ParamFormulaType number1, ParamFormulaType number2);

  /**
   * Create a formula representing the division of two operands.
   *
   * <p>If the denumerator evaluates to zero (division-by-zero), either directly as value or
   * indirectly via an additional constraint, then the solver is allowed to choose an arbitrary
   * value for the result of the division (cf. SMTLIB standard for the division operator in Ints or
   * Reals theory).
   *
   * <p>Note: Some solvers, e.g., Yices2, abort with an exception when exploring a division-by-zero
   * during the SAT-check. This is not compliant to the SMTLIB standard, but sadly happens.
   */
  ResultFormulaType divide(ParamFormulaType numerator, ParamFormulaType denumerator);

  ResultFormulaType multiply(ParamFormulaType number1, ParamFormulaType number2);

  // ----------------- Numeric relations, return type BooleanFormula -----------------

  BooleanFormula equal(ParamFormulaType number1, ParamFormulaType number2);

  /** all given numbers are pairwise unequal. */
  BooleanFormula distinct(List<ParamFormulaType> pNumbers);

  BooleanFormula greaterThan(ParamFormulaType number1, ParamFormulaType number2);

  BooleanFormula greaterOrEquals(ParamFormulaType number1, ParamFormulaType number2);

  BooleanFormula lessThan(ParamFormulaType number1, ParamFormulaType number2);

  BooleanFormula lessOrEquals(ParamFormulaType number1, ParamFormulaType number2);

  /**
   * The {@code floor} operation returns the nearest integer formula that is less or equal to the
   * given argument formula.
   *
   * <p>For rational formulae, SMTlib2 denotes this operation as {@code to_int}.
   */
  IntegerFormula floor(ParamFormulaType formula);
}
