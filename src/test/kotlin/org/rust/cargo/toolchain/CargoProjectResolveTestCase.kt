package org.rust.cargo.toolchain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import org.assertj.core.api.Assertions.assertThat
import org.rust.cargo.RustWithToolchainTestCaseBase
import org.rust.cargo.project.CargoProjectDescription
import org.rust.cargo.project.settings.toolchain
import org.rust.cargo.project.workspace.CargoProjectWorkspace
import org.rust.cargo.project.workspace.CargoProjectWorkspaceListener
import org.rust.cargo.util.getServiceOrThrow

class CargoProjectResolveTestCase : RustWithToolchainTestCaseBase() {
    override val dataPath: String = "src/test/resources/org/rust/cargo/toolchain/fixtures"

    fun testResolveExternalLibrary() = withProject("external_library") {
        module.messageBus
            .connect()
            .subscribe(
                CargoProjectWorkspace.UPDATES,
                object: CargoProjectWorkspaceListener {
                    override fun onProjectUpdated(projectDescription: CargoProjectDescription) {
                        val reference = extractReference("src/main.rs")
                        assertThat(reference.resolve()).isNotNull()
                    }
                })

        updateCargoProject()
    }

    fun testResolveLocalPackage() = withProject("local_package") {
        module.messageBus
            .connect()
            .subscribe(
                CargoProjectWorkspace.UPDATES,
                object: CargoProjectWorkspaceListener {
                    override fun onProjectUpdated(projectDescription: CargoProjectDescription) {
                        val reference = extractReference("src/main.rs")
                        assertThat(reference.resolve()).isNotNull()
                    }
                })

        updateCargoProject()
    }

    private fun updateCargoProject() {
        module.getServiceOrThrow<CargoProjectWorkspace>().requestUpdate(module.toolchain!!)
    }

    private fun extractReference(path: String): PsiReference {
        val vFile = LocalFileSystem.getInstance().findFileByPath("${myProject.basePath}/$path")!!
        val psiFile = PsiManager.getInstance(myProject).findFile(vFile)!!
        val documentManager = PsiDocumentManager.getInstance(project)

        var referenceOffset = 0
        WriteCommandAction.runWriteCommandAction(project) {
            val document = documentManager.getDocument(psiFile)!!
            val text = document.text
            val refTag = "<ref>"
            referenceOffset = text.indexOf(refTag)
            document.deleteString(referenceOffset, referenceOffset + refTag.length)
            documentManager.commitDocument(document)
        }
        check(referenceOffset > 0)

        return psiFile.findReferenceAt(referenceOffset)!!
    }
}