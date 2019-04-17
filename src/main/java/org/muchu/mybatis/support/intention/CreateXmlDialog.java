package org.muchu.mybatis.support.intention;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class CreateXmlDialog extends DialogWrapper {

    private final JLabel myInformationLabel = new JLabel(CodeInsightBundle.message("dialog.create.class.label", "xml"));

    private final JLabel myPackageLabel = new JLabel(CodeInsightBundle.message("dialog.create.class.destination.package.label"));

    private final ReferenceEditorComboWithBrowseButton myPackageComponent;

    private final JTextField myTfClassName = new MyTextField();

    private final Project myProject;

    private PsiDirectory myTargetDirectory;

    private final String myXmlName;

    private final DestinationFolderComboBox myDestinationCB = new DestinationFolderComboBox() {
        @Override
        public String getTargetPackage() {
            return myPackageComponent.getText().trim();
        }

        @Override
        protected boolean reportBaseInTestSelectionInSource() {
            return CreateXmlDialog.this.reportBaseInTestSelectionInSource();
        }

        @Override
        protected boolean reportBaseInSourceSelectionInTest() {
            return CreateXmlDialog.this.reportBaseInSourceSelectionInTest();
        }
    };

    @NonNls
    private static final String RECENTS_KEY = "CreateXmlDialog.RecentsKey";

    public CreateXmlDialog(@NotNull Project project,
                           @NotNull String title,
                           @NotNull String targetXmlName,
                           @NotNull String targetPackageName,
                           boolean classNameEditable,
                           @Nullable Module defaultModule) {
        super(project, true);
        myXmlName = targetXmlName;
        myProject = project;
        myPackageComponent = new PackageNameReferenceEditorCombo(targetPackageName, project, RECENTS_KEY, title);
        myPackageComponent.setTextFieldPreferredWidth(40);
        init();
        setTitle(title);
        myTfClassName.setText(myXmlName);
    }

    protected boolean reportBaseInTestSelectionInSource() {
        return false;
    }

    protected boolean reportBaseInSourceSelectionInTest() {
        return false;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTfClassName;
    }

    @Override
    protected JComponent createCenterPanel() {
        return new JPanel(new BorderLayout());
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();

        gbConstraints.insets = JBUI.insets(4, 8);
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor = GridBagConstraints.WEST;

        gbConstraints.weightx = 0;
        gbConstraints.gridwidth = 1;
        panel.add(myInformationLabel, gbConstraints);
        gbConstraints.insets = JBUI.insets(4, 8);
        gbConstraints.gridx = 1;
        gbConstraints.weightx = 1;
        gbConstraints.gridwidth = 1;
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor = GridBagConstraints.WEST;
        panel.add(myTfClassName, gbConstraints);

        myTfClassName.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                getOKAction().setEnabled(PsiNameHelper.getInstance(myProject).isIdentifier(myTfClassName.getText()));
            }
        });
        getOKAction().setEnabled(StringUtil.isNotEmpty(myXmlName));

        gbConstraints.gridx = 0;
        gbConstraints.gridy = 2;
        gbConstraints.weightx = 0;
        gbConstraints.gridwidth = 1;
        panel.add(myPackageLabel, gbConstraints);

        gbConstraints.gridx = 1;
        gbConstraints.weightx = 1;

        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myPackageComponent.getButton().doClick();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)), myPackageComponent.getChildComponent());

        JPanel _panel = new JPanel(new BorderLayout());
        _panel.add(myPackageComponent, BorderLayout.CENTER);
        panel.add(_panel, gbConstraints);

        gbConstraints.gridy = 3;
        gbConstraints.gridx = 0;
        gbConstraints.gridwidth = 2;
        gbConstraints.insets.top = 12;
        gbConstraints.anchor = GridBagConstraints.WEST;
        gbConstraints.fill = GridBagConstraints.NONE;
        final JBLabel label = new JBLabel(RefactoringBundle.message("target.destination.folder"));
        panel.add(label, gbConstraints);

        gbConstraints.gridy = 4;
        gbConstraints.gridx = 0;
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.insets.top = 4;
        panel.add(myDestinationCB, gbConstraints);

        final boolean isMultipleSourceRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(myProject).size() > 1;
        myDestinationCB.setVisible(isMultipleSourceRoots);
        label.setVisible(isMultipleSourceRoots);
        label.setLabelFor(myDestinationCB);
        return panel;
    }

    public PsiDirectory getTargetDirectory() {
        return myTargetDirectory;
    }

    private String getPackageName() {
        String name = myPackageComponent.getText();
        return name != null ? name.trim() : "";
    }

    private static class MyTextField extends JTextField {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            FontMetrics fontMetrics = getFontMetrics(getFont());
            size.width = fontMetrics.charWidth('a') * 40;
            return size;
        }
    }

    @Override
    protected void doOKAction() {
        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, myPackageComponent.getText());
        final String packageName = getPackageName();

        final String[] errorString = new String[1];
        CommandProcessor.getInstance().executeCommand(myProject, () -> {
            try {
                final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
                final MoveDestination destination = myDestinationCB.selectDirectory(targetPackage, false);
                if (destination == null) return;
                myTargetDirectory = WriteAction.compute(() -> {
                    PsiDirectory baseDir = getBaseDir(packageName);
                    if (baseDir == null && destination instanceof MultipleRootsMoveDestination) {
                        errorString[0] = "Destination not found for package '" + packageName + "'";
                        return null;
                    }
                    return destination.getTargetDirectory(baseDir);
                });
                if (myTargetDirectory == null) {
                    return;
                }
                errorString[0] = RefactoringMessageUtil.checkCanCreateFile(myTargetDirectory, getXmlName() + ".xml");
            } catch (IncorrectOperationException e) {
                errorString[0] = e.getMessage();
            }
        }, CodeInsightBundle.message("create.directory.command"), null);

        if (errorString[0] != null) {
            if (errorString[0].length() > 0) {
                Messages.showMessageDialog(myProject, errorString[0], CommonBundle.getErrorTitle(), Messages.getErrorIcon());
            }
            return;
        }
        super.doOKAction();
    }

    @Nullable
    protected PsiDirectory getBaseDir(String packageName) {
        return null;
    }

    @NotNull
    public String getXmlName() {
        return myXmlName;
    }

}