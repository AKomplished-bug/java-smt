/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.java_smt.solvers.mathsat5;

import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5FormulaManager.getMsatTerm;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_all_sat;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_assert_formula;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_check_sat_with_assumptions;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_get_unsat_assumptions;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_get_unsat_core;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_last_error_message;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_push_backtrack_point;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolationHandle;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.LongArrayBackedList;
import org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.AllSatModelCallback;

class Mathsat5TheoremProver extends Mathsat5AbstractProver implements ProverEnvironment {

  private final ShutdownNotifier shutdownNotifier;

  Mathsat5TheoremProver(
      Mathsat5SolverContext pMgr,
      ShutdownNotifier pShutdownNotifier,
      Mathsat5FormulaCreator creator,
      Set<ProverOptions> options) {

    super(pMgr, createConfig(options), creator);
    shutdownNotifier = pShutdownNotifier;
  }

  private static Map<String, String> createConfig(Set<ProverOptions> opts) {
    return ImmutableMap.<String, String>builder()
        .put("model_generation", opts.contains(ProverOptions.GENERATE_MODELS) ? "true" : "false")
        .put(
            "unsat_core_generation",
            opts.contains(ProverOptions.GENERATE_UNSAT_CORE)
                    || opts.contains(ProverOptions.GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS)
                ? "1"
                : "0")
        .build();
  }

  @Override
  @Nullable
  public InterpolationHandle addConstraint(BooleanFormula constraint) {
    Preconditions.checkState(!closed);
    msat_assert_formula(curEnv, getMsatTerm(constraint));
    return null;
  }

  @Override
  public void push() {
    Preconditions.checkState(!closed);
    msat_push_backtrack_point(curEnv);
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    Preconditions.checkState(!closed);
    long[] terms = msat_get_unsat_core(curEnv);
    List<BooleanFormula> result = new ArrayList<>(terms.length);
    for (long t : terms) {
      result.add(creator.encapsulateBoolean(t));
    }
    return result;
  }

  @Override
  public <T> T allSat(AllSatCallback<T> callback, List<BooleanFormula> important)
      throws InterruptedException, SolverException {
    Preconditions.checkState(!closed);
    long[] imp = new long[important.size()];
    int i = 0;
    for (BooleanFormula impF : important) {
      imp[i++] = getMsatTerm(impF);
    }
    MathsatAllSatCallback<T> uCallback = new MathsatAllSatCallback<>(callback);
    push();
    int numModels = msat_all_sat(curEnv, imp, uCallback);
    pop();

    if (numModels == -1) {
      throw new SolverException(
          "Error occurred during Mathsat allsat: " + msat_last_error_message(curEnv));

    } else if (numModels == -2) {
      // Formula is trivially tautological.
      // With the current API, we have no way of signaling this except by iterating over all 2^n
      // models, which is probably not what we want.
      throw new UnsupportedOperationException("allSat for trivially tautological formula");
    }
    return callback.getResult();
  }

  class MathsatAllSatCallback<T> implements AllSatModelCallback {
    private final AllSatCallback<T> clientCallback;

    MathsatAllSatCallback(AllSatCallback<T> pClientCallback) {
      clientCallback = pClientCallback;
    }

    @Override
    public void callback(long[] model) throws InterruptedException {
      shutdownNotifier.shutdownIfNecessary();
      clientCallback.apply(
          new LongArrayBackedList<BooleanFormula>(model) {
            @Override
            protected BooleanFormula convert(long pE) {
              return creator.encapsulateBoolean(pE);
            }
          });
    }
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    Preconditions.checkState(!closed);
    return !msat_check_sat_with_assumptions(
        curEnv, Mathsat5FormulaManager.getMsatTerm(assumptions));
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      Collection<BooleanFormula> assumptions) throws SolverException, InterruptedException {

    if (!isUnsatWithAssumptions(assumptions)) {
      return Optional.empty();
    }
    long[] unsatAssumptions = msat_get_unsat_assumptions(curEnv);
    return Optional.of(
        Arrays.stream(unsatAssumptions)
            .mapToObj(creator::encapsulateBoolean)
            .collect(Collectors.toList()));
  }
}
