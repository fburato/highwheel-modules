# Release process

## Prerequisites

- Install `gpg` and `pinentry`
- Define in `$HOME/sbt/1.0/sonatype.sbt` the following content (replace sonatype password):
  ```scala
  credentials += Credentials("Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        "fburato",
        "$SONATYPE_PASSWORD")
  ```
- Import the gpg key from the keystore (entry `GPG key`)

## Process

Run `make release`
