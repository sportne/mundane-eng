.PHONY: test native-smoke native-validator validator-verify native-formatter formatter-verify native-trace trace-verify native-boundaries native-suite package-native-suite native-suite-verify boundary-isolation ci-workflow-evidence ci-workflow-verify integrated-toolchain-trial multi-author-layout-trial operational-scale-trial independent-conformance-compare native-reqif-probe identity-continuity-trial verification-model-trial safety-classification-trial diagnostic-presentation-trial allocation-model-trial glossary-symbol-trial trace-policy-trial verify

BUILD_ROOT := build/maintained
CLASS_DIR := $(BUILD_ROOT)/classes
NATIVE_SMOKE := $(BUILD_ROOT)/native-smoke
VALIDATE_NATIVE := $(BUILD_ROOT)/mundanereq-validate
FORMAT_NATIVE := $(BUILD_ROOT)/mundanereq-format
TRACE_NATIVE := $(BUILD_ROOT)/mundanereq-trace
REQIF_PROBE := experiments/0006-reqif-interchange/build/reqifprobe
NATIVE_IMAGE ?= native-image
override NATIVE_IMAGE_FLAGS := -O0 --no-fallback -march=compatibility
GRAALVM_HOME = $(shell candidate="$$(readlink -f "$$(command -v $(NATIVE_IMAGE))")"; \
	while test "$$candidate" != /; do \
		candidate="$$(dirname "$$candidate")"; \
		if test -f "$$candidate/LICENSE_NATIVEIMAGE.txt" && test -d "$$candidate/legal"; then \
			echo "$$candidate"; \
			break; \
		fi; \
	done)
include versions.properties
GENERATED_DIR := $(BUILD_ROOT)/generated
PACKAGE_NAME := mundanereq-native-suite-$(SUITE_VERSION)-linux-x86_64-glibc2.34
PACKAGE_DIR := $(BUILD_ROOT)/package
PACKAGE_STAGE := $(PACKAGE_DIR)/$(PACKAGE_NAME)
PACKAGE_ARCHIVE := $(PACKAGE_DIR)/$(PACKAGE_NAME).tar.gz
PACKAGE_ARCHIVE_CHECKSUM := $(PACKAGE_ARCHIVE).sha256
EXPECTED_PACKAGE_DIR := $(abspath build/maintained/package)
MAIN_SOURCES := $(shell find src/main/java -type f -name '*.java' -print | LC_ALL=C sort)
TEST_SOURCES := $(shell find src/test/java -type f -name '*.java' -print | LC_ALL=C sort)

YAML_JAR := build/dependencies/snakeyaml-engine-3.1.1.jar
CLASSPATH := $(CLASS_DIR):$(YAML_JAR)

.PHONY: yaml-dependency
yaml-dependency:
	scripts/fetch-yaml-parser.sh

test: yaml-dependency version-declarations
	mkdir -p $(CLASS_DIR)
	javac -cp $(YAML_JAR) --release 21 -Xlint:all -Werror -d $(CLASS_DIR) $(MAIN_SOURCES) $(GENERATED_DIR)/mundanereq/Versions.java $(TEST_SOURCES)
	java -ea -cp $(CLASSPATH) mundanereq.test.MaintainedTestSuite

native-smoke: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(NATIVE_SMOKE)) mundanereq.smoke.MaintainedBuildTest
	$(NATIVE_SMOKE)

native-validator: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(VALIDATE_NATIVE)) mundanereq.cli.ValidatorMain
	$(VALIDATE_NATIVE) --version

validator-verify: native-validator
	java -ea -cp $(CLASSPATH) mundanereq.cli.ValidatorVerificationTest $(VALIDATE_NATIVE)

native-formatter: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(FORMAT_NATIVE)) mundanereq.cli.FormatterMain
	$(FORMAT_NATIVE) --version

formatter-verify: native-formatter
	java -ea -cp $(CLASSPATH) mundanereq.cli.FormatterVerificationTest $(FORMAT_NATIVE)

native-trace: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(TRACE_NATIVE)) mundanereq.cli.TraceMain
	$(TRACE_NATIVE) --version

trace-verify: native-trace
	java -ea -cp $(CLASSPATH) mundanereq.cli.TraceVerificationTest $(TRACE_NATIVE)

native-boundaries: native-validator native-formatter native-trace

native-suite: native-boundaries

package-native-suite: native-suite
	test "$(abspath $(PACKAGE_DIR))" = "$(EXPECTED_PACKAGE_DIR)"
	test "$(abspath $(PACKAGE_STAGE))" = "$(EXPECTED_PACKAGE_DIR)/$(PACKAGE_NAME)"
	test "$$(uname -s)" = Linux
	test "$$(uname -m)" = x86_64
	test "$$(getconf GNU_LIBC_VERSION | sed 's/ .*//')" = glibc
	test -f "$(GRAALVM_HOME)/LICENSE_NATIVEIMAGE.txt"
	test -d "$(GRAALVM_HOME)/legal"
	@for binary in "$(VALIDATE_NATIVE)" "$(FORMAT_NATIVE)" "$(TRACE_NATIVE)"; do \
		maximum="$$(objdump -T "$$binary" | sed -n 's/.*GLIBC_\([0-9][0-9.]*\).*/\1/p' | sort -V | tail -n 1)"; \
		test -n "$$maximum"; \
		test "$$(printf '%s\n' "$$maximum" 2.34 | sort -V | tail -n 1)" = 2.34 || { echo "$$binary requires GLIBC_$$maximum, above package ceiling GLIBC_2.34" >&2; exit 1; }; \
	done
	rm -rf "$(abspath $(PACKAGE_STAGE))"
	mkdir -p "$(PACKAGE_STAGE)/bin" "$(PACKAGE_STAGE)/docs/contracts" "$(PACKAGE_STAGE)/LICENSES/GraalVM-JDK"
	install -m 755 "$(VALIDATE_NATIVE)" "$(FORMAT_NATIVE)" "$(TRACE_NATIVE)" "$(PACKAGE_STAGE)/bin/"
	install -m 644 distribution/README.md "$(PACKAGE_STAGE)/README.md"
	install -m 644 distribution/validate.md distribution/format.md distribution/trace.md distribution/THIRD-PARTY-NOTICES.md "$(PACKAGE_STAGE)/docs/"
	install -m 644 specification/0007-validator-trial-contract-0.1.md specification/0008-formatter-trial-contract-0.1.md specification/0009-trace-trial-contract-0.1.md "$(PACKAGE_STAGE)/docs/contracts/"
	install -m 644 LICENSE "$(PACKAGE_STAGE)/LICENSES/mundanereq-BSD-3-Clause.txt"
	install -m 644 dependencies/SnakeYAML-Engine-LICENSE.txt "$(PACKAGE_STAGE)/LICENSES/"
	install -m 644 dependencies/README.md "$(PACKAGE_STAGE)/LICENSES/YAML-DEPENDENCY.md"
	install -m 644 specification/0013-compiled-diagnostic-rules.md specification/0016-diagnostic-recovery.md specification/0017-sarif-validation-output.md "$(PACKAGE_STAGE)/docs/contracts/"
	install -m 644 specification/0010-requirements-yaml-0.3.md specification/0011-tool-safety-and-yaml-commands.md "$(PACKAGE_STAGE)/docs/contracts/"
	mkdir -p "$(PACKAGE_STAGE)/docs/contracts/schema"
	install -m 644 specification/schema/requirements-yaml-0.3.json "$(PACKAGE_STAGE)/docs/contracts/schema/"
	install -m 644 "$(GRAALVM_HOME)/LICENSE_NATIVEIMAGE.txt" "$(PACKAGE_STAGE)/LICENSES/GraalVM-Native-Image.txt"
	cp -R "$(GRAALVM_HOME)/legal/." "$(PACKAGE_STAGE)/LICENSES/GraalVM-JDK/"
	install -m 644 $(GENERATED_DIR)/versions.json "$(PACKAGE_STAGE)/VERSIONS.json"
	$(NATIVE_IMAGE) --version > "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	uname -srm >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	javac -version >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt" 2>&1
	gcc --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	ldd --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	make --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	tar --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	sha256sum --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	objdump --version | sed -n '1p' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	getconf GNU_LIBC_VERSION >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	echo 'Native Image CPU target: compatibility (x86-64 baseline)' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	echo 'Package glibc symbol ceiling: GLIBC_2.34' >> "$(PACKAGE_STAGE)/BUILD-ENVIRONMENT.txt"
	sha256sum "$(PACKAGE_STAGE)"/bin/* | sed 's#  $(PACKAGE_STAGE)/#  #' > "$(PACKAGE_STAGE)/SHA256SUMS"
	tar --sort=name --mtime='UTC 1970-01-01' --owner=0 --group=0 --numeric-owner -czf "$(PACKAGE_ARCHIVE)" -C "$(PACKAGE_DIR)" "$(PACKAGE_NAME)"
	sha256sum "$(PACKAGE_ARCHIVE)" | sed 's#  .*/#  #' > "$(PACKAGE_ARCHIVE_CHECKSUM)"

native-suite-verify: package-native-suite
	java -ea -cp $(CLASSPATH) mundanereq.distribution.NativeSuiteVerificationTest "$(PACKAGE_STAGE)" "$(PACKAGE_ARCHIVE)" "$(PACKAGE_ARCHIVE_CHECKSUM)" "$(GRAALVM_HOME)"

boundary-isolation: native-boundaries
	java -ea -cp $(CLASSPATH) mundanereq.boundary.NativeBoundaryIsolationTest \
		$(VALIDATE_NATIVE) $(FORMAT_NATIVE) $(TRACE_NATIVE)

ci-workflow-evidence:
	java -ea -cp $(CLASSPATH) mundanereq.ci.CiWorkflowVerificationTest \
		$(FORMAT_NATIVE) $(VALIDATE_NATIVE) $(TRACE_NATIVE)

ci-workflow-verify: native-suite ci-workflow-evidence

integrated-toolchain-trial: native-suite
	java -ea -cp $(CLASSPATH) mundanereq.trial.IntegratedToolchainTrialTest \
		$(FORMAT_NATIVE) $(VALIDATE_NATIVE) $(TRACE_NATIVE)

multi-author-layout-trial: native-suite
	java -ea -cp $(CLASSPATH) mundanereq.trial.MultiAuthorLayoutTrialTest \
		$(FORMAT_NATIVE) $(VALIDATE_NATIVE) $(TRACE_NATIVE)

operational-scale-trial: native-suite
	experiments/0014-operational-scale/run.sh \
		$(FORMAT_NATIVE) $(VALIDATE_NATIVE) $(TRACE_NATIVE) $(BUILD_ROOT)/operational-scale

independent-conformance-compare: native-validator
	experiments/0015-independent-conformance/compare.rb \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/independent-conformance

native-reqif-probe:
	$(MAKE) -C experiments/0006-reqif-interchange native

identity-continuity-trial: native-validator native-trace native-reqif-probe
	$(RM) -r $(BUILD_ROOT)/identity-continuity
	experiments/0016-identity-continuity/run.sh \
		$(VALIDATE_NATIVE) $(TRACE_NATIVE) $(REQIF_PROBE) $(BUILD_ROOT)/identity-continuity

verification-model-trial: native-validator
	$(RM) -r $(BUILD_ROOT)/verification-model
	experiments/0017-verification-evidence/run.sh \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/verification-model

safety-classification-trial: native-validator
	$(RM) -r $(BUILD_ROOT)/safety-classification
	experiments/0018-safety-classification/run.sh \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/safety-classification

diagnostic-presentation-trial: native-validator
	$(RM) -r $(BUILD_ROOT)/diagnostic-presentation
	experiments/0019-diagnostic-presentation/run.sh \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/diagnostic-presentation

allocation-model-trial: native-validator
	$(RM) -r $(BUILD_ROOT)/allocation-model
	experiments/0020-allocation-model/run.sh \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/allocation-model

glossary-symbol-trial: native-validator
	$(RM) -r $(BUILD_ROOT)/glossary-symbols
	experiments/0021-glossary-symbols/run.sh \
		$(VALIDATE_NATIVE) $(BUILD_ROOT)/glossary-symbols

trace-policy-trial: native-validator native-trace
	$(RM) -r $(BUILD_ROOT)/trace-policies
	experiments/0022-trace-policies/run.sh \
		$(VALIDATE_NATIVE) $(TRACE_NATIVE) $(BUILD_ROOT)/trace-policies

verify: yaml-schema-verify test native-smoke boundary-isolation validator-verify formatter-verify trace-verify native-suite-verify ci-workflow-verify integrated-toolchain-trial multi-author-layout-trial independent-conformance-compare

.PHONY: native-migrate yaml-verify
native-migrate: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-migrate) mundanereq.cli.MigrateMain
	$(BUILD_ROOT)/mundanereq-migrate --version

yaml-verify: native-suite native-migrate
	java -ea -cp $(CLASSPATH) mundanereq.cli.YamlVerificationTest $(VALIDATE_NATIVE) $(FORMAT_NATIVE) $(TRACE_NATIVE) $(BUILD_ROOT)/mundanereq-migrate

verify: yaml-verify

.PHONY: yaml-schema-verify
yaml-schema-verify:
	scripts/check-yaml-schema.sh

verify: yaml-schema-verify

.PHONY: version-declarations version-verify
version-declarations:
	python3 scripts/generate-versions.py versions.properties $(GENERATED_DIR)

version-verify: test
	python3 scripts/check-versions.py "$(SUITE_VERSION)"

verify: version-verify

.PHONY: native-compile compiled-verify
native-compile: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-compile) mundanereq.cli.CompileMain
	$(BUILD_ROOT)/mundanereq-compile --version

compiled-verify: native-compile
	python3 scripts/check-compiled-artifacts.py $(BUILD_ROOT)/mundanereq-compile

verify: compiled-verify

.PHONY: compilation-experiment-verify
compilation-experiment-verify: test
	python3 experiments/0027-compilation-linking/run.py

verify: compilation-experiment-verify

.PHONY: native-link link-verify
native-link: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASS_DIR) -o $(abspath $(BUILD_ROOT)/mundane-link) engineering.artifacts.LinkMain
	$(BUILD_ROOT)/mundane-link --version

link-verify: native-link native-compile
	python3 scripts/check-artifact-linking.py $(BUILD_ROOT)/mundane-link

verify: link-verify

.PHONY: native-plan native-verification verification-verify
native-plan: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASS_DIR) -o $(abspath $(BUILD_ROOT)/mundane-plan) engineering.verification.PlanMain
	$(BUILD_ROOT)/mundane-plan --version

native-verification: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASS_DIR) -o $(abspath $(BUILD_ROOT)/mundane-verify) engineering.verification.VerifyMain
	$(BUILD_ROOT)/mundane-verify --version

verification-verify: native-plan native-verification native-compile
	python3 scripts/check-verification-workflow.py $(BUILD_ROOT)/mundane-plan $(BUILD_ROOT)/mundane-verify

verify: verification-verify

.PHONY: report-verify
report-verify: native-verification
	python3 experiments/0029-verification-report/run.py

verify: report-verify

.PHONY: recovery-verify
recovery-verify: native-validator native-formatter native-compile
	python3 scripts/check-parser-recovery.py $(BUILD_ROOT)

verify: recovery-verify

.PHONY: sarif-verify
sarif-verify: native-validator yaml-schema-verify
	build/schema-check-venv/bin/python scripts/check-sarif.py $(VALIDATE_NATIVE)

verify: sarif-verify

.PHONY: workflow-corpus-verify
workflow-corpus-verify: native-compile native-formatter native-plan native-verification
	python3 experiments/0033-workflow-regressions/run.py
	python3 experiments/0033-workflow-regressions/mutations.py

verify: workflow-corpus-verify

.PHONY: native-work work-verify
native-work: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-work) engineering.work.WorkMain
	$(BUILD_ROOT)/mundane-work --version

work-verify: native-work
	python3 scripts/check-work-items.py $(BUILD_ROOT)/mundane-work

verify: work-verify

.PHONY: work-index work-backlog-verify
work-index: native-work
	python3 scripts/work-backlog.py --write

work-backlog-verify: native-work
	python3 experiments/0034-work-items/migrate.py
	python3 experiments/0035-work-yaml/migrate.py
	python3 scripts/work-backlog.py
	python3 scripts/check-planning-docs.py

verify: work-backlog-verify

.PHONY: work-regression-verify
work-regression-verify: native-work
	python3 experiments/0034-work-items/regressions.py
	python3 experiments/0035-work-yaml/regressions.py

verify: work-regression-verify

.PHONY: work-yaml-verify
work-yaml-verify: native-work
	scripts/check-work-yaml.sh

verify: work-yaml-verify

.PHONY: attribute-validate-verify
attribute-validate-verify: native-validator
	scripts/check-attribute-schema.sh
	python3 scripts/check-attributes.py

verify: attribute-validate-verify

.PHONY: attribute-format-verify
attribute-format-verify: native-formatter native-trace
	python3 scripts/check-attribute-format.py

verify: attribute-format-verify

.PHONY: attribute-compile-verify
attribute-compile-verify: native-compile
	python3 scripts/check-attribute-compile.py

verify: attribute-compile-verify

.PHONY: attribute-link-verify
attribute-link-verify: native-compile native-plan native-link native-verification native-work
	python3 scripts/check-attribute-link.py

verify: attribute-link-verify
