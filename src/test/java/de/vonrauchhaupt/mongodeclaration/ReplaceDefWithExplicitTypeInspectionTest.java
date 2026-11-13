package de.vonrauchhaupt.mongodeclaration;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class ReplaceDefWithExplicitTypeInspectionTest extends BasePlatformTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(new ReplaceDefWithExplicitTypeInspection());
    }

    public void testDefWithArrayListReplacedByList() {
        myFixture.configureByText("Test.groovy", "def<caret> items = new java.util.ArrayList<String>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'List<String>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("List<String> items = new java.util.ArrayList<String>()");
    }

    public void testVarWithInteger() {
        myFixture.configureByText("Test.groovy", "var<caret> count = 42");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'Integer'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("Integer count = 42");
    }

    public void testDefWithStarImportDoesNotDuplicate() {
        myFixture.addFileToProject("com/example/CustomService.java", "package com.example; public class CustomService {}");
        myFixture.configureByText("Test.groovy", "import com.example.*\n\ndef<caret> service = new com.example.CustomService()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'CustomService'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("import com.example.*\n\nCustomService service = new com.example.CustomService()");
    }

    public void testDefWithSamePackageDoesNotAddImport() {
        myFixture.addFileToProject("com/example/CustomService.java", "package com.example; public class CustomService {}");
        myFixture.configureByText("Test.groovy", "package com.example\n\ndef<caret> service = new CustomService()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'CustomService'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("package com.example\n\nCustomService service = new CustomService()");
    }

    public void testDefWithMapGenericCustomClasses() {
        myFixture.addFileToProject("com/example/Key.java", "package com.example; public class Key {}");
        myFixture.addFileToProject("com/example/Value.java", "package com.example; public class Value {}");
        myFixture.configureByText("Test.groovy", "def<caret> map = new java.util.HashMap<com.example.Key, com.example.Value>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'HashMap<Key, Value>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("import com.example.Key\nimport com.example.Value\n\nHashMap<Key, Value> map = new java.util.HashMap<com.example.Key, com.example.Value>()");
    }

    public void testVarWithLinkedListReplacedByList() {
        myFixture.configureByText("Test.groovy", "var<caret> items = new java.util.LinkedList<Integer>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'List<Integer>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("List<Integer> items = new java.util.LinkedList<Integer>()");
    }

    public void testDefWithHashSetReplacedBySet() {
        myFixture.configureByText("Test.groovy", "def<caret> items = new java.util.HashSet<String>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'Set<String>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("Set<String> items = new java.util.HashSet<String>()");
    }

    public void testVarWithTreeSetReplacedBySet() {
        myFixture.configureByText("Test.groovy", "var<caret> items = new java.util.TreeSet<Double>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'Set<Double>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("Set<Double> items = new java.util.TreeSet<Double>()");
    }

    public void testVarWithLinkedHashSetReplacedBySet() {
        myFixture.configureByText("Test.groovy", "var<caret> items = new java.util.LinkedHashSet<Long>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'Set<Long>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("Set<Long> items = new java.util.LinkedHashSet<Long>()");
    }

    public void testDefWithListLiteralReplacedByList() {
        myFixture.configureByText("Test.groovy", "def<caret> items = [1, 2, 3]");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'List<Integer>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("List<Integer> items = [1, 2, 3]");
    }

    public void testDefWithString() {
        myFixture.configureByText("Test.groovy", "def<caret> name = \"Groovy\"");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'String'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("String name = \"Groovy\"");
    }

    public void testDefWithCustomClassAddsImport() {
        myFixture.addFileToProject("com/example/CustomService.java", "package com.example; public class CustomService {}");
        myFixture.configureByText("Test.groovy", "def<caret> service = new com.example.CustomService()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'CustomService'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("import com.example.CustomService\n\nCustomService service = new com.example.CustomService()");
    }

    public void testDefWithCustomClassAlreadyImportedDoesNotDuplicate() {
        myFixture.addFileToProject("com/example/CustomService.java", "package com.example; public class CustomService {}");
        myFixture.configureByText("Test.groovy", "import com.example.CustomService\n\ndef<caret> service = new com.example.CustomService()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'CustomService'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("import com.example.CustomService\n\nCustomService service = new com.example.CustomService()");
    }

    public void testDefWithListGenericCustomClassAddsImport() {
        myFixture.addFileToProject("com/example/CustomEntity.java", "package com.example; public class CustomEntity {}");
        myFixture.configureByText("Test.groovy", "def<caret> list = new java.util.ArrayList<com.example.CustomEntity>()");
        IntentionAction intention = myFixture.findSingleIntention("Replace with 'List<CustomEntity>'");
        assertNotNull("Expected quick fix to be available", intention);
        myFixture.launchAction(intention);
        myFixture.checkResult("import com.example.CustomEntity\n\nList<CustomEntity> list = new java.util.ArrayList<com.example.CustomEntity>()");
    }
}
