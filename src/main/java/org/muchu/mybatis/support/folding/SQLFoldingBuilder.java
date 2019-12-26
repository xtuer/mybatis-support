package org.muchu.mybatis.support.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.muchu.mybatis.support.bean.Include;
import org.muchu.mybatis.support.bean.Mapper;
import org.muchu.mybatis.support.bean.Sql;
import org.muchu.mybatis.support.bean.Statement;

import java.util.ArrayList;
import java.util.List;

public class SQLFoldingBuilder extends FoldingBuilderEx {


    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        FoldingGroup foldingGroup = FoldingGroup.newGroup("mybatis");
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        if (!(root instanceof XmlFile)) {
            return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
        }
        XmlFile xmlFile = (XmlFile) root;
        DomFileElement<Mapper> element = DomManager.getDomManager(root.getProject()).getFileElement(xmlFile, Mapper.class);
        if (element != null) {
            List<Statement> statements = element.getRootElement().getStatements();
            for (Statement statement : statements) {
                List<Include> includes = statement.getIncludes();
                for (Include include : includes) {
                    if (include.getRefId() == null) {
                        continue;
                    }
                    List<Sql> sqlList = element.getRootElement().getSQL();
                    for (Sql sql : sqlList) {
                        if (StringUtils.equals(sql.getId() == null ? "" : sql.getId().getStringValue(), include.getRefId().getStringValue())) {
                            descriptors.add(new FoldingDescriptor(include.getXmlElement().getNode(),
                                    new TextRange(include.getXmlElement().getTextRange().getStartOffset() + 1,
                                            include.getXmlElement().getTextRange().getEndOffset() - 1),
                                    foldingGroup) {
                                @Nullable
                                @Override
                                public String getPlaceholderText() {
                                    String valueOf = sql.getValue();
                                    return valueOf == null ? "" : valueOf;
                                }
                            });
                        }
                    }
                }
            }
        }
        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        System.out.println(node);
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }
}