package io.kaif.rank;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

public class SortingNodeTest {

  @Test
  public void emptyRoot() throws Exception {
    SortingNode<Long> emptyRoot = SortingNode.emptyRoot();
    assertFalse(emptyRoot.hasChild());
    assertTrue(emptyRoot.isRoot());
    assertFalse(emptyRoot.hasParent());
  }

  @Test
  public void toStringShouldNotIncludeParent() throws Exception {
    SortingNode<Integer> tree = new SortingNode.Builder<Integer>()//
        .childNode(200).siblingNode(210).siblingNode(220).build();
    assertNotNull("include parent in toString will go infinite loop", tree.toString());
  }

  @Test
  public void buildNodeTree() throws Exception {

    SortingNode<String> root = new SortingNode.Builder<String>().build();
    assertFalse(root.hasChild());
    assertTrue(root.isRoot());
    assertFalse(root.hasParent());

    SortingNode<Integer> tree = new SortingNode.Builder<Integer>()//
        .childNode(100)
        .siblingNode(200)
        .childNode(210)
        .siblingNode(220)
        .parent()
        .siblingNode(300)
        .siblingNode(400)
        .childNode(410)
        .siblingNode(420)
        .siblingNode(430)
        .childNode(431)
        .build();

    assertFalse(tree.hasParent());
    assertTrue(tree.isRoot());
    assertFalse(tree.hasParent());

    String expected = ""
        + "*\n"
        + "  100\n"
        + "  200\n"
        + "    210\n"
        + "    220\n"
        + "  300\n"
        + "  400\n"
        + "    410\n"
        + "    420\n"
        + "    430\n"
        + "      431";
    assertEquals(expected, tree.prettyPrint());

    SortingNode<Integer> n410 = tree.getChildren().get(3).getChildren().get(0);
    assertFalse(n410.hasChild());
    assertTrue(n410.hasParent());
    assertFalse(n410.isRoot());

    assertEquals("410", n410.prettyPrint());
  }

  @Test
  public void depthFirst() throws Exception {

    assertEquals(Collections.<Integer>emptyList(),
        SortingNode.<Integer>emptyRoot().depthFirst().collect(toList()));

    SortingNode<Integer> tree = new SortingNode.Builder<Integer>()//
        .childNode(100)
        .siblingNode(200)
        .childNode(210)
        .siblingNode(220)
        .parent()
        .siblingNode(300)
        .siblingNode(400)
        .childNode(410)
        .siblingNode(420)
        .siblingNode(430)
        .childNode(431)
        .build();

    assertEquals(asList(100, 200, 210, 220, 300, 400, 410, 420, 430, 431),
        tree.depthFirst().collect(toList()));

    SortingNode<Integer> n400 = tree.getChildren().get(3);
    assertEquals(asList(400, 410, 420, 430, 431), n400.depthFirst().collect(toList()));
  }

  @Test
  public void breathFirst() throws Exception {

    assertEquals(Collections.<Integer>emptyList(),
        SortingNode.<Integer>emptyRoot().breathFirst().collect(toList()));

    SortingNode<Integer> tree = new SortingNode.Builder<Integer>()//
        .childNode(100)
        .siblingNode(200)
        .childNode(210)
        .siblingNode(220)
        .parent()
        .siblingNode(300)
        .siblingNode(400)
        .childNode(410)
        .siblingNode(420)
        .siblingNode(430)
        .childNode(431)
        .build();

    assertEquals(asList(100, 200, 300, 400, 210, 220, 410, 420, 430, 431),
        tree.breathFirst().collect(toList()));

    SortingNode<Integer> n200 = tree.getChildren().get(1);
    assertEquals(asList(200, 210, 220), n200.breathFirst().collect(toList()));
  }

  @Test
  public void deepSort() throws Exception {

    SortingNode<Integer> tree = new SortingNode.Builder<Integer>()//
        .childNode(200)
        .siblingNode(100)
        .childNode(110)
        .siblingNode(120)
        .parent()
        .siblingNode(400)
        .childNode(410)
        .siblingNode(430)
        .childNode(431)
        .parent()
        .siblingNode(420)
        .parent()
        .siblingNode(300)
        .build();

    tree = tree.deepSort((a, b) -> b.getValue() - a.getValue());
    String expected = ""
        + "*\n"
        + "  400\n"
        + "    430\n"
        + "      431\n"
        + "    420\n"
        + "    410\n"
        + "  300\n"
        + "  200\n"
        + "  100\n"
        + "    120\n"
        + "    110";
    assertEquals(expected, tree.prettyPrint());
  }
}