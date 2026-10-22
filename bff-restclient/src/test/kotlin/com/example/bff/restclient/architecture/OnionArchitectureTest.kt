package com.example.bff.restclient.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture

/**
 * DDD + オニオンアーキテクチャの依存ルールを機械的に検証する。
 *
 * 層の依存方向（外側から内側へのみ依存する）:
 *  - presentation → application → domain
 *  - infrastructure → domain（application には依存しない）
 *  - domain は他層・Spring・Jackson・Reactor に依存しない（bff-webclient と違い Reactor も持ち込まない）
 *
 * ドメインの純粋性は layeredArchitecture では表現できないため、別ルールで許可パッケージを限定する。
 */
@AnalyzeClasses(
    packages = ["com.example.bff.restclient"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class OnionArchitectureTest {

    @ArchTest
    val `layers respect the onion dependency direction`: ArchRule =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Presentation").definedBy("..presentation..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
            // presentation は例外型（domain.exception）を参照して HTTP 404 / 502 に変換するため、Domain へのアクセスを許可する。
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()

    @ArchTest
    val `domain depends only on itself and the JDK and Kotlin`: ArchRule =
        classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "..domain..",
                "java..",
                "kotlin..",
                "org.jetbrains.annotations..",
            )
}
