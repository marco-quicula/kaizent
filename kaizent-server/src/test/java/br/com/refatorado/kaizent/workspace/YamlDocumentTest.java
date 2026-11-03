package br.com.refatorado.kaizent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlDocumentTest {

  @Test
  void splitsTheFrontMatterFromTheBody() {
    YamlDocument.FrontMatter document =
        YamlDocument.frontMatter("---\nid: T-1\n---\n\nThe body.\n").orElseThrow();

    assertThat(document.fields()).isEqualTo(Map.of("id", "T-1"));
    assertThat(document.body()).isEqualTo("The body.");
  }

  @Test
  void ignoresAByteOrderMarkAndWindowsLineEndings() {
    YamlDocument.FrontMatter document =
        YamlDocument.frontMatter("﻿---\r\nid: T-1\r\n---\r\nBody\r\n").orElseThrow();

    assertThat(document.fields()).containsEntry("id", "T-1");
    assertThat(document.body()).isEqualTo("Body");
  }

  @Test
  void closesOnlyOnALineThatIsExactlyThreeDashes() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> YamlDocument.frontMatter("---\nid: T-1\n---not-a-delimiter\nBody\n"))
        .withMessage("the front matter is not closed with a --- line");
    assertThat(YamlDocument.frontMatter("---\nid: T-1\n---  \n").orElseThrow().body()).isEmpty();
  }

  @Test
  void acceptsAnEmptyFrontMatter() {
    YamlDocument.FrontMatter document = YamlDocument.frontMatter("---\n---\nBody").orElseThrow();

    assertThat(document.fields()).isEmpty();
    assertThat(document.body()).isEqualTo("Body");
  }

  @Test
  void hasNoFrontMatterWhenTheFirstLineIsNotThreeDashes() {
    assertThat(YamlDocument.frontMatter("# Title\n---\nid: T-1\n---\n")).isEmpty();
    assertThat(YamlDocument.frontMatter("--- \nid: T-1\n---\n")).isEmpty();
  }

  @Test
  void rejectsFrontMatterThatIsNotAMapping() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> YamlDocument.frontMatter("---\n- a\n- b\n---\nBody"))
        .withMessage("not a YAML mapping");
  }
}
