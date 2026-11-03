# Security Policy

## Supported Versions

Only the latest minor release branch receives security patches and vulnerability remediation.

| Version | Supported |
| :--- | :---: |
| 1.7.x | :white_check_mark: |
| < 1.7.0 | :x: |

---

## Reporting a Vulnerability

We take the security and privacy of Gemini Pro users seriously. If you discover a vulnerability or potential security risk in this application, please follow responsible disclosure practices:

1. **Do not open a public issue.**
2. Report the vulnerability privately via:
   - **GitHub Security Advisories**: Navigate to the repository's **Security** tab and click **"Report a vulnerability"** (preferred).
   - Alternatively, reach out directly to the repository maintainers.

### What to Include in Your Report
To help us triage and resolve the issue quickly, please include:
- A clear description of the vulnerability and its potential impact.
- Step-by-step instructions or proof-of-concept (PoC) to reproduce the issue.
- Android OS version(s) and device model(s) tested.
- Any suggested mitigations or patches, if available.

### Disclosure Timeline
- **Acknowledgment**: Within 48 hours of receiving your report.
- **Triage & Assessment**: Usually within 5 business days.
- **Fix & Public Release**: As soon as a verified patch is tested across supported Android versions.
- **Public Disclosure**: Coordinated after the fix has been published in a release.

---

## Key Security Controls & Architecture

Gemini Pro adheres to modern Android security and privacy standards:

* **WebView Hardening**: `allowFileAccess`, `allowFileAccessFromFileURLs`, and `allowUniversalAccessFromFileURLs` are strictly disabled. Deprecated web database flags are removed.
* **Scoped Storage Compliance**: File downloads utilize `MediaStore.Downloads` on Android 10+ (API 29+), eliminating legacy storage permissions and isolating media downloads within Android's system storage framework.
* **Intent Sanitization**: Deep-links and `intent://` scheme URLs are sanitized with `Intent.CATEGORY_BROWSABLE`, with component and selector nullification to prevent unauthorized IPC or redirection to internal app components.
* **JavaScript Bridge Protection**: All `@JavascriptInterface` bridges enforce rigorous payload validation, error isolation, and string escaping (`JSONObject.quote`) to prevent script injection and IPC crashes.
* **Component Exposure**: Only `MainActivity` is exported (as required for the app launcher). All other components and internal `FileProvider` paths are unexported (`android:exported="false"`).

For a detailed technical evaluation and vulnerability assessment, please refer to our [Security Audit Report](docs/SECURITY_AUDIT.md).
