## 📝 Description

<!-- Briefly describe the changes introduced by this PR. Include the problem it solves and the phase it belongs to. -->
- **Phase Target:** <!-- e.g., Phase 2: Analyzer -->
- **Summary:** <!-- Short summary of changes -->

## 🔗 Related Issues

<!-- Link to any relevant issues (e.g., "Fixes #123"). -->

## 🛠️ Implementation Details

<!-- Outline the technical approach. Why was it implemented this way? -->

## 🛑 Strict Agent Rules Checklist

<!-- Please verify that all the following architectural rules are met before requesting a review. -->

- [ ] **Phase Discipline:** This PR implements *only* the current authorized phase. No future phases were implemented prematurely.
- [ ] **No Placeholders:** There is no fake functionality, dummy stubs, or placeholder implementations claiming to work.
- [ ] **Immutability:** Domain objects and requirements remain immutable where practical.
- [ ] **Safe Analysis:** (If applicable) Project analysis does not blindly execute arbitrary project code or trigger unauthorized downloads.
- [ ] **Testing:** Every meaningful new feature is covered by unit tests (without relying on live internet access).

## ✅ Definition of Done

- [ ] Code compiles successfully.
- [ ] All tests pass locally (`mvn clean test`).
- [ ] Public APIs are documented.
- [ ] No obvious dead code remains.
- [ ] The architecture remains highly modular and decoupled.

## 📸 Screenshots / Output

<!-- If this PR changes CLI output or adds a new `.jbe` syntax, provide an example below. -->
```shell
# Example CLI output or .jbe snippet here
```
