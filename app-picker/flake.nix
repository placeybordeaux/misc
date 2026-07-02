{
  description = "Color App Picker — Android build environment (JDK 17 + Android SDK + Gradle)";

  # Tarball URL (codeload) instead of github: ref to avoid the GitHub API rate limit.
  inputs.nixpkgs.url = "https://github.com/NixOS/nixpkgs/archive/refs/heads/nixos-24.05.tar.gz";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-darwin" "aarch64-darwin" "x86_64-linux" "aarch64-linux" ];
      forAll = f: nixpkgs.lib.genAttrs systems (system: f system);
    in
    {
      devShells = forAll (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };

          # Versions pinned to app/build.gradle (compileSdk 34, build-tools 34.0.0)
          # and the Gradle wrapper (gradle 8.2 / AGP 8.2.0).
          android = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "34" ];
            buildToolsVersions = [ "34.0.0" ];
            cmdLineToolsVersion = "11.0";
            includeEmulator = false;
            includeSystemImages = false;
            includeSources = false;
            includeNDK = false;
          };

          androidSdk = android.androidsdk;
          sdkRoot = "${androidSdk}/libexec/android-sdk";
          jdk = pkgs.jdk17;

          # AGP 8.2's JdkImageTransform runs `jlink` from the JVM that Gradle
          # itself runs on. nixpkgs' gradle defaults to JDK 21, whose jlink fails
          # on android-34's core-for-system-modules.jar. Pin Gradle to JDK 17.
          gradle = pkgs.gradle.override { java = jdk; };

          # On Linux the AGP-bundled aapt2 is a prebuilt ELF that won't run under
          # Nix's loader, so point Gradle at the SDK's patched aapt2. On Darwin the
          # downloaded mach-o binary runs natively, so the override is skipped.
          aapt2Override = pkgs.lib.optionalString pkgs.stdenv.isLinux
            "-Pandroid.aapt2FromMavenOverride=${sdkRoot}/build-tools/34.0.0/aapt2";
        in
        {
          default = pkgs.mkShell {
            # fdroidserver drives the self-hosted F-Droid repo (`fdroid update`);
            # gettext provides envsubst for rendering the F-Droid config template.
            packages = [ jdk gradle androidSdk pkgs.fdroidserver pkgs.gettext ];

            JAVA_HOME = "${jdk}";
            ANDROID_HOME = sdkRoot;
            ANDROID_SDK_ROOT = sdkRoot;

            shellHook = ''
              echo "Color App Picker dev shell"
              echo "  JDK:          ${jdk}"
              echo "  ANDROID_HOME: ${sdkRoot}"
              echo "  gradle:       $(gradle --version 2>/dev/null | awk '/^Gradle/{print $2}')"
              echo
              echo "Build:  gradle assembleDebug ${aapt2Override}"
              echo "Test:   gradle testDebugUnitTest ${aapt2Override}"
            '';
          };
        });
    };
}
