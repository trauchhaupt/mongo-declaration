package de.vonrauchhaupt.mongodeclaration;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;

import java.util.*;
import java.util.stream.Collectors;

public class ReplaceDefWithExplicitTypeInspection extends LocalInspectionTool {

    private static final Set<String> DEFAULT_GROOVY_PACKAGES = Set.of(
            "java.lang",
            "java.util",
            "java.io",
            "java.net",
            "groovy.lang",
            "groovy.util"
    );

    private static final Set<String> DEFAULT_GROOVY_CLASSES = Set.of(
            "java.math.BigInteger",
            "java.math.BigDecimal"
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Replace 'def' or 'var' with explicit type";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "ReplaceDefWithExplicitType";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GroovyPsiElementVisitor(new GroovyElementVisitor() {
            @Override
            public void visitVariableDeclaration(@NotNull GrVariableDeclaration variableDeclaration) {
                if (variableDeclaration.getTypeElementGroovy() != null) {
                    return;
                }

                GrModifierList modifierList = variableDeclaration.getModifierList();
                if (modifierList == null) {
                    return;
                }

                PsiElement targetModifierElement = findDynamicModifierElement(modifierList);
                if (targetModifierElement == null) {
                    return;
                }

                CommonTypeInfo explicitType = inferCommonExplicitType(variableDeclaration.getVariables());
                if (explicitType == null) {
                    return;
                }

                String keyword = targetModifierElement.getText();
                holder.registerProblem(
                        targetModifierElement,
                        "Replace '" + keyword + "' with explicit type '" + explicitType.presentableText() + "'",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        new ReplaceDefWithExplicitTypeQuickFix(
                                explicitType.presentableText(),
                                explicitType.canonicalText(),
                                explicitType.classesToImport()
                        )
                );
            }
        });
    }

    @Nullable
    private PsiElement findDynamicModifierElement(@NotNull GrModifierList modifierList) {
        for (PsiElement modifier : modifierList.getModifiers()) {
            String text = modifier.getText();
            if ("def".equals(text) || "var".equals(text)) {
                return modifier;
            }
        }
        return null;
    }

    @Nullable
    private CommonTypeInfo inferCommonExplicitType(GrVariable[] variables) {
        if (variables == null || variables.length == 0) {
            return null;
        }

        List<PsiType> inferredTypes = Arrays.stream(variables)
                .map(GrVariable::getTypeGroovy)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (inferredTypes.size() != variables.length) {
            return null;
        }

        List<CommonTypeInfo> normalizedTypes = inferredTypes.stream()
                .map(this::normalizeToPreferredDeclarationType)
                .collect(Collectors.toList());

        List<String> canonicalTypeTexts = normalizedTypes.stream()
                .map(CommonTypeInfo::canonicalText)
                .collect(Collectors.toList());

        List<String> presentableTypeTexts = normalizedTypes.stream()
                .map(CommonTypeInfo::presentableText)
                .collect(Collectors.toList());

        for (String typeText : presentableTypeTexts) {
            if (typeText.isBlank() || "Object".equals(typeText)) {
                return null;
            }
        }

        if (canonicalTypeTexts.stream().distinct().count() != 1) {
            return null;
        }

        if (presentableTypeTexts.stream().distinct().count() != 1) {
            return null;
        }

        return normalizedTypes.get(0);
    }

    @NotNull
    private CommonTypeInfo normalizeToPreferredDeclarationType(@NotNull PsiType type) {
        if (type instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass != null) {
                if (isCollectionInheritor(psiClass, "java.util.List")) {
                    return toCollectionInterfaceTypeInfo(classType, psiClass, "List", "java.util.List");
                }
                if (isCollectionInheritor(psiClass, "java.util.Set")) {
                    return toCollectionInterfaceTypeInfo(classType, psiClass, "Set", "java.util.Set");
                }
            }
        }

        List<PsiClass> classesToImport = new ArrayList<>();
        collectClassesToImport(type, classesToImport);
        return new CommonTypeInfo(type.getPresentableText(), type.getCanonicalText(), classesToImport);
    }

    private boolean isCollectionInheritor(@NotNull PsiClass psiClass, @NotNull String targetQName) {
        if (targetQName.equals(psiClass.getQualifiedName())) {
            return true;
        }
        return InheritanceUtil.isInheritor(psiClass, targetQName);
    }

    @NotNull
    private CommonTypeInfo toCollectionInterfaceTypeInfo(@NotNull PsiClassType classType,
                                                         @NotNull PsiClass resolvedClass,
                                                         @NotNull String shortInterfaceName,
                                                         @NotNull String canonicalInterfaceName) {
        List<PsiClass> classesToImport = new ArrayList<>();
        PsiClass interfaceClass = JavaPsiFacade.getInstance(resolvedClass.getProject())
                .findClass(canonicalInterfaceName, resolvedClass.getResolveScope());
        if (interfaceClass == null) {
            interfaceClass = JavaPsiFacade.getInstance(resolvedClass.getProject())
                    .findClass(canonicalInterfaceName, GlobalSearchScope.allScope(resolvedClass.getProject()));
        }
        if (interfaceClass != null) {
            classesToImport.add(interfaceClass);
        }

        PsiType[] parameters = classType.getParameters();
        for (PsiType parameter : parameters) {
            collectClassesToImport(parameter, classesToImport);
        }

        if (parameters.length == 0) {
            return new CommonTypeInfo(shortInterfaceName, canonicalInterfaceName, classesToImport);
        }

        String presentableParameters = Arrays.stream(parameters)
                .map(PsiType::getPresentableText)
                .collect(Collectors.joining(", "));
        String canonicalParameters = Arrays.stream(parameters)
                .map(PsiType::getCanonicalText)
                .collect(Collectors.joining(", "));

        return new CommonTypeInfo(
                shortInterfaceName + "<" + presentableParameters + ">",
                canonicalInterfaceName + "<" + canonicalParameters + ">",
                classesToImport
        );
    }

    private void collectClassesToImport(@Nullable PsiType type, @NotNull List<PsiClass> result) {
        if (type == null) {
            return;
        }
        if (type instanceof PsiArrayType arrayType) {
            collectClassesToImport(arrayType.getComponentType(), result);
        } else if (type instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass != null) {
                result.add(psiClass);
            }
            for (PsiType param : classType.getParameters()) {
                collectClassesToImport(param, result);
            }
        }
    }

    private record CommonTypeInfo(String presentableText, String canonicalText, List<PsiClass> classesToImport) {
    }

    private static class ReplaceDefWithExplicitTypeQuickFix implements LocalQuickFix {
        private final String explicitTypeText;
        private final String canonicalTypeText;
        private final List<PsiClass> classesToImport;

        public ReplaceDefWithExplicitTypeQuickFix(@NotNull String explicitTypeText,
                                                  @NotNull String canonicalTypeText,
                                                  @NotNull List<PsiClass> classesToImport) {
            this.explicitTypeText = explicitTypeText;
            this.canonicalTypeText = canonicalTypeText;
            this.classesToImport = classesToImport;
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getFamilyName() {
            return "Replace 'def' or 'var' with explicit type";
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getName() {
            return "Replace with '" + explicitTypeText + "'";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement modifierElement = descriptor.getPsiElement();
            if (modifierElement == null) {
                return;
            }
            GrVariableDeclaration variableDeclaration = com.intellij.psi.util.PsiTreeUtil.getParentOfType(modifierElement, GrVariableDeclaration.class);
            if (variableDeclaration == null) {
                return;
            }
            GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
            GrTypeElement newTypeElement = factory.createTypeElement(explicitTypeText);
            GrModifierList modifierList = variableDeclaration.getModifierList();
            if (modifierList != null) {
                variableDeclaration.addAfter(newTypeElement, modifierList);
                modifierElement.delete();
            }
            PsiFile file = variableDeclaration.getContainingFile();
            if (file instanceof GroovyFile groovyFile) {
                for (PsiClass psiClass : classesToImport) {
                    if (isImportNeeded(groovyFile, psiClass)) {
                        groovyFile.addImportForClass(psiClass);
                    }
                }
            }
        }

        private boolean isImportNeeded(@NotNull GroovyFile file, @Nullable PsiClass psiClass) {
            if (psiClass == null) {
                return false;
            }
            String qName = psiClass.getQualifiedName();
            if (qName == null || qName.isBlank()) {
                return false;
            }
            if (DEFAULT_GROOVY_CLASSES.contains(qName)) {
                return false;
            }
            String packageName = "";
            int lastDot = qName.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = qName.substring(0, lastDot);
            }
            if (packageName.isEmpty()) {
                return false;
            }
            if (packageName.equals(file.getPackageName())) {
                return false;
            }
            if (DEFAULT_GROOVY_PACKAGES.contains(packageName)) {
                return false;
            }
            for (GrImportStatement importStatement : file.getImportStatements()) {
                if (importStatement.isOnDemand()) {
                    var ref = importStatement.getImportReference();
                    if (ref != null && packageName.equals(ref.getQualifiedReferenceName())) {
                        return false;
                    }
                } else {
                    var ref = importStatement.getImportReference();
                    if (ref != null && qName.equals(ref.getQualifiedReferenceName())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}