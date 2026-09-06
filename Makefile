BUILD_ROOT := build/maintained
CLASS_DIR := $(BUILD_ROOT)/classes
NATIVE_SMOKE := $(BUILD_ROOT)/native-smoke
VALIDATE_NATIVE := $(BUILD_ROOT)/mundanereq-validate
FORMAT_NATIVE := $(BUILD_ROOT)/mundanereq-format
TRACE_NATIVE := $(BUILD_ROOT)/mundanereq-trace
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
	rm -rf $(CLASS_DIR)
	mkdir -p $(CLASS_DIR)
	javac -cp $(YAML_JAR) --release 21 -Xlint:all -Werror -d $(CLASS_DIR) $(MAIN_SOURCES) $(GENERATED_DIR)/mundanereq/Versions.java $(TEST_SOURCES)
	java -ea -cp $(CLASSPATH) mundanereq.test.MaintainedTestSuite


.PHONY: native-validator
native-validator: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-validate) mundanereq.cli.ValidatorMain
	$(BUILD_ROOT)/mundanereq-validate --version

.PHONY: native-formatter
native-formatter: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-format) mundanereq.cli.FormatterMain
	$(BUILD_ROOT)/mundanereq-format --version

.PHONY: native-trace
native-trace: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-trace) mundanereq.cli.TraceMain
	$(BUILD_ROOT)/mundanereq-trace --version

.PHONY: native-compile
native-compile: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundanereq-compile) mundanereq.cli.CompileMain
	$(BUILD_ROOT)/mundanereq-compile --version

.PHONY: native-plan
native-plan: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-plan) engineering.verification.PlanMain
	$(BUILD_ROOT)/mundane-plan --version

.PHONY: native-link
native-link: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-link) engineering.artifacts.LinkMain
	$(BUILD_ROOT)/mundane-link --version

.PHONY: native-verification
native-verification: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-verify) engineering.verification.VerifyMain
	$(BUILD_ROOT)/mundane-verify --version

.PHONY: native-work
native-work: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-work) engineering.work.WorkMain
	$(BUILD_ROOT)/mundane-work --version

.PHONY: native-impact
native-impact: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-impact) engineering.impact.ImpactMain
	$(BUILD_ROOT)/mundane-impact --version

native-suite: native-validator native-formatter native-trace

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
	install -m 644 specification/schema/requirements-yaml-0.3.json specification/schema/requirements-yaml-0.4.json specification/schema/attribute-declaration-0.1.json "$(PACKAGE_STAGE)/docs/contracts/schema/"
	install -m 644 specification/0020-project-attributes-yaml-0.4.md "$(PACKAGE_STAGE)/docs/contracts/"
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

.PHONY: verify native-suite package-native-suite yaml-schema-verify version-declarations version-verify work-index work-backlog-verify work-verify work-yaml-verify attribute-validate-verify attribute-format-verify attribute-compile-verify attribute-link-verify attribute-report-verify attribute-workflow-verify impact-verify impact-workflow-verify native-suite-verify yaml-verify
version-declarations:
	python3 scripts/generate-versions.py versions.properties $(GENERATED_DIR)
	python3 scripts/editor-versions.py
yaml-schema-verify:
	scripts/check-yaml-schema.sh
version-verify: test
	python3 scripts/check-versions.py "$(SUITE_VERSION)"
work-index: native-work
	python3 scripts/work-backlog.py --write
work-backlog-verify: native-work
	python3 scripts/work-backlog.py
	python3 scripts/check-planning-docs.py
work-verify: native-work
	python3 scripts/check-work-items.py $(BUILD_ROOT)/mundane-work
work-yaml-verify: native-work
	scripts/check-work-yaml.sh
attribute-validate-verify: native-validator
	scripts/check-attribute-schema.sh
	python3 scripts/check-attributes.py
attribute-format-verify: native-formatter native-trace
	python3 scripts/check-attribute-format.py
attribute-compile-verify: native-compile native-validator native-formatter native-trace
	python3 scripts/check-attribute-compile.py
	python3 scripts/check-cli-delimiters.py
attribute-link-verify: native-compile native-plan native-link native-verification native-work
	python3 scripts/check-attribute-link.py
attribute-report-verify: native-verification
	python3 scripts/check-attribute-report.py
attribute-workflow-verify: native-validator native-formatter native-compile native-plan native-link native-verification
	python3 scripts/check-attribute-corpus.py
	python3 experiments/0036-project-attributes/workflow.py
	python3 experiments/0036-project-attributes/regressions.py
impact-verify: native-impact
	java -ea -cp $(CLASS_DIR) engineering.impact.ImpactCliTest $(BUILD_ROOT)/mundane-impact
	java -ea -cp $(CLASS_DIR) engineering.impact.ImpactViewTest $(BUILD_ROOT)/mundane-impact
impact-workflow-verify: native-compile native-plan native-work native-impact
	python3 experiments/0037-impact-analysis/regressions.py
	python3 experiments/0037-impact-analysis/mutations.py
native-suite-verify: package-native-suite
	python3 scripts/check-native-package.py $(PACKAGE_STAGE) $(PACKAGE_ARCHIVE)
yaml-verify: native-validator native-formatter native-trace native-compile
	python3 scripts/check-yaml-workflow.py
verify: yaml-schema-verify test yaml-verify native-suite-verify version-verify work-verify work-yaml-verify work-backlog-verify attribute-validate-verify attribute-format-verify attribute-compile-verify attribute-link-verify attribute-report-verify attribute-workflow-verify impact-verify impact-workflow-verify editor-verify installed-editor-verify

.PHONY: native-editor editor-dependencies editor-vsix editor-verify package-editor installed-editor-verify
native-editor: test
	$(NATIVE_IMAGE) $(NATIVE_IMAGE_FLAGS) -cp $(CLASSPATH) -o $(abspath $(BUILD_ROOT)/mundane-editor) mundanereq.editor.EditorMain
editor-dependencies:
	cd editors/vscode && npm ci
editor-vsix: version-declarations editor-dependencies
	cd editors/vscode && npm run package
editor-verify: native-editor editor-vsix
	python3 scripts/check-editor-bridge.py
	cd editors/vscode && npm run test:unit && xvfb-run -a npm test && xvfb-run -a node test/traffic-run.js
package-editor: native-editor editor-vsix
	python3 scripts/package-editor.py "$(GRAALVM_HOME)"
	python3 scripts/check-editor-package.py "$(GRAALVM_HOME)"
installed-editor-verify: package-editor
	cd editors/vscode && xvfb-run -a node test/installed-run.js
