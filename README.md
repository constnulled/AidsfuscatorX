> [!IMPORTANT]
> **Community Continuation & Attribution Notice**
> 
> **AidsfuscatorX** is an unofficial, enhanced continuation of **[Aidsfuscator](https://github.com/LvStrnggg/aidsfuscator)**, originally created and developed by **[LvStrnggg](https://github.com/LvStrnggg)** (`lvstrng` on Discord).
> 
**AidsfuscatorX** was created to implement experimental new transformers, additional obfuscation features, and bug fixes.
> 
> * **Original Creator:** [LvStrnggg](https://github.com/LvStrnggg)
> * **Upstream Repository:** [LvStrnggg/aidsfuscator](https://github.com/LvStrnggg/aidsfuscator)
> * **Disclaimer:** All credit for everything goes to `LvStrnggg`.

# Aidsfuscator v2.x
Aidsfuscator is a java bytecode obfuscator that aims to become one of, if not the best free obfuscators. 
<br>
  Join the [discord server](https://discord.gg/4JGANqEZsK)!
</br>

### IF YOU'RE PLANNING TO CONTRIBUTE, SCROLL DOWN AND READ THE README!!!

## Features
- Trimming
- Name obfuscation (optional aggressive overloading!)
- Control Flow Flattening
- Control Flow Shuffling
- LocalVariableTable clearing
- LineNumberTable mutation
- Method salting
- Class salting
- Integer encryption
- String Encryption (with anti-tamper + concatenation obfuscation)
- Reference obfuscation (fields and methods)
- Reference hiding 
- Custom dictionary support
- Fat JAR support
- CLI
- Config system
- Exclusion system with annotations
- Exclusion preset system
- Automatic `fabric.mod.json` handling
- Annotations API

## Why?
Over these 2 years of me working with Java bytecode, I noticed that there's not many good free Java bytecode obfuscator options out there, so I decided to make one myself. It is also a passion project.

## Why v2?
I made a poll on [Aidsfuscators/Cryptics discord server](https://discord.gg/4JGANqEZsK), asking if anyone wanted me to rewrite the v1 aidsfuscator. The majority said yes, so that's what makes me want to do this project.

## How to use?
- Download the zip file from [the releases page](https://github.com/LvStrnggg/aidsfuscator/releases).
- Extract the ZIP file
- Your config.json and exclusions.json files should stay in your workspace folder, everything else can stay outside.
- Run the obfuscator with Java 25: `java -jar aidsfuscator.jar --config=config.json --exclusions=exclusions.json`

### Default structure

```
aidsfuscator.jar
workspace
|- aidsfuscator-api.jar
|- config.json
|- initOrder.java
|- references.json
\- exclusions.json
```

### What not to do
- Do not run it with `java -jar aidsfuscator --config=workspace/config.json --exclusions=workspace/exclusions.json`. Aidsfuscator automatically prefixes `workspace/` before any workspace item, that isn't the libs folder or input file.

### Aidsfuscator API
Starting from v2.9.0, Aidsfuscator now has an API JAR file with a few annotations. This JAR can be added to your projects dependencies and you can annotate methods, fields and classes with these annotations to exclude them from obfuscation. These annotations get removed in the Aidsfuscators post-processor.

## Using the obfuscator
### Exclusions and inclusions
You can add exclusions and inclusions with the follow format in the exclusions file:
```json
{
  "token": {
    "class": [
      "example/Exclusion",
      "!example/Inclusion"
    ],
    "field": [
      "example/Exclusion.excludedMethod Ljava/lang/String;",
      "!example/Inclusions.includedMethod Ljava/lang/String;"
    ],
    "method": [ 
      "example/Exclusion.excludedMethod(IJZBDFSCLjava/lang/String;)V",
      "!example/Inclusion.includedMethod(IJZBDFSCLjava/lang/String;)V"
    ],
    "annotation": [
      "example/ExampleAnnotation"
    ]
  }
}
```

Replace `token` with any of these:
- `global` (Excludes a class globally from all obfuscation. Applies to classes only)
- `renameClass` (Excludes a class from being renamed. Applies to classes only)
- `renameField` (Excludes a field from being renamed. Applies to classes and fields)
- `renameMethod` (Excludes a method from being renamed. Applies to classes and methods)
- `localNames` (Excludes LVT (Local Variable Table) from getting its names cleared. Applies to classes and methods)
- `lineNumbers` (Excludes LNT (Line Number Table) from getting obfuscated. Applies to classes and methods)
- `trim` (Excludes trimming of a specific member. Applies to classes, fields and methods)
- `methodSalting` (Excludes a method from being salted. Applies to classes and methods)
- `classSalting` (Excludes a class from being salted. Applies to classes only)
- `referenceObfuscate` (Excludes a method from getting references obfuscated in it. Applies to classes and methods)
- `fixConstants` (Excludes a field from getting its value moved to the static initializer. Applies to classes and fields)
- `integerEncrypt` (Excludes a class or method from getting its integer values encrypted. Applies to classes and methods)
- `stringEncrypt` (Excludes a class or method from getting its string literals encrypted. Applies to classes and methods)
- `controlFlowFlatten` (Excludes a method from getting its control flow flattened into a switch. Applies to classes and methods)
- `controlFlowShuffle` (Excludes a method from getting its control flow shuffled. Applies to classes and methods)

If you wanted to exclude every method with the name "test" and with any parameters with return type `void` from getting Control Flow Flattening and Integer Encryption applied to it, your exclusion file would look like this:
```json
{
  "controlFlowFlatten": {
    "method": [
      "*.test(*)V"
    ]
  },
  "integerEncrypt": {
    "method": [
      "*.test(*)V"
    ]
  }
}
```

### Class Initialization Order
Class Initialization Order allows you to strengthen class salts by specifying pairs of classes that execute in order. The `initOrder.json` file is a JSON array of JSON arrays,
but you have to add only two classes in the second array. For example:
If you are sure that `pkg.Class2` first gets initialized by `pkg.Class1` and you add it to the file, your file should look like this:

```json
[
  ["pkg.Class1", "pkg.Class2"]
]
```

You can add multiple of these class pairs and you can chain them, but you have to be certain that the order is true, otherwise wrong values may get outputted.

### Reference Obfuscation Inclusions
Reference Obfuscation Inclusions is a file that specifies reference obfuscation candidates. It uses the same matching system as in exclusions. Examples:
#### Methods
- Any method called `test` in any class with any parameters with any return type
  <br>
  `*.test(*)*`
  </br>
- Any method called `test` in any class with NO parameters with `void` return type
  <br>
  `*.test()V`
  </br>
- Any method called `test` in class `pkg.TestClass` with first parameter `long` with `java.lang.String` return type
  <br>
  `pkg/TestClass.test(J*)Ljava/lang/String;`
  </br>
- Any method called `test` in any class that ends with `subpkg.TestClass` with last parameter `boolean` with `int` return type
  <br>
  `*/subpkg/TestClass.test(*Z)I`
  </br>
- Any method called `test` in any class that's under a package called `pkg` with any parameters and return value (shortened)
  <br>
  `*/pkg/*.test(*`
  </br>

#### Fields
- Any field called `test` in any class with any return type
  <br>
  `*.test *`
  </br>
- Any field called `test` in any class with `boolean` return type
  <br>
  `*.test Z`
  </br>
- Any field called `test` in class `pkg.TestClass` with `java.lang.String` return type
  <br>
  `pkg/TestClass.test Ljava/lang/String;`
  </br>
- Any field called `test` in any class that ends with `subpkg.TestClass` with `int` return type
  <br>
  `*/subpkg/TestClass.test I`
  </br>
- Any field called `test` in any class that's under a package called `pkg` with any return value
  <br>
  `*/pkg/*.test *`
  </br>
Keep in mind: for fields, separate return type from name with a space.

## Contributing
Since I've received some pull requests that don't live up to my expectations, I wan't to avoid wasting other peoples' time, so:
When contributing, I expect bug fixes rather than new features. I can decide on features myself: what's good and what's not.

<div>
  If you're thinking about adding a feature, please contact me on discord `lvstrng` to talk it out. Or create an issue as a suggestion, stating what you want to see getting added into the obfuscator. One good example was: instead of forking the repo, writing the transformer and creating a pull request a guy made an issue as a suggestion, stating that they want to see Zelix-style flow in the obfuscator. I told the user this is probably not needed, because the current control flow obfuscation already does its magic. This user avoided wasting tons of time on something that I wouldn't agree on either way.
</div>

# AidsfuscatorX

---


### TL;DR
If you're thinking about adding new features to obfuscator, contact me on discord or make a suggestion. Otherwise, please only push bug fixes.
