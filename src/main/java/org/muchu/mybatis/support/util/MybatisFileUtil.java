package org.muchu.mybatis.support.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.muchu.mybatis.support.bean.Mapper;

public class MybatisFileUtil {

    public static boolean isMybatisFile(VirtualFile file, Project project) {
        if (project == null || !project.isOpen()) {
            return false;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(file);
        if (!(psiFile instanceof XmlFile)) {
            return false;
        }
        DomFileElement<Mapper> element = DomManager.getDomManager(project).getFileElement((XmlFile) psiFile, Mapper.class);
        return element != null;
    }
}