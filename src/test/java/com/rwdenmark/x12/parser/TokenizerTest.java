package com.rwdenmark.x12.parser;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenizerTest {

    private static final String ISA = "ISA*00*          *00*          *ZZ*SENDER         "
            + "*ZZ*RECEIVER       *240101*1200*^*00501*000000001*0*P*:~";

    private static final String COMPACT_ISA =
            "ISA*00**00**ZZ*PREMIER*ZZ*KEYINS*061015*1023*^*00501*000000001*0*P*:~";

    @Test
    void delimitersAreReadFromIsa() {
        Delimiters d = Delimiters.fromIsa(ISA);
        assertThat(d.element()).isEqualTo('*');
        assertThat(d.segment()).isEqualTo('~');
        assertThat(d.subElement()).isEqualTo(':');
        assertThat(d.repetition()).isEqualTo('^');
    }

    @Test
    void delimitersReadFromCompactIsaWithoutPadding() {
        Delimiters d = Delimiters.fromIsa(COMPACT_ISA);
        assertThat(d.element()).isEqualTo('*');
        assertThat(d.segment()).isEqualTo('~');
        assertThat(d.subElement()).isEqualTo(':');
        assertThat(d.repetition()).isEqualTo('^');
    }

    @Test
    void tokenizerSplitsSegmentsAndElements() {
        String raw = ISA + "GS*HC*S*R*20240101*1200*1*X*005010X222A1~ST*837*0001*005010X222A1~SE*1*0001~GE*1*1~IEA*1*000000001~";
        Delimiters d = Delimiters.fromIsa(raw);
        List<Segment> segments = Tokenizer.tokenize(raw, d);

        assertThat(segments).extracting(Segment::id)
                .containsExactly("ISA", "GS", "ST", "SE", "GE", "IEA");
        assertThat(segments.get(2).element(0)).isEqualTo("837");
        assertThat(segments.get(2).element(2)).isEqualTo("005010X222A1");
    }

    @Test
    void tokenizesCompactInterchange() {
        String raw = COMPACT_ISA
                + "GS*HC*S*R*20240101*1200*1*X*005010X222A1~ST*837*0001*005010X222A1~SE*1*0001~GE*1*1~IEA*1*000000001~";
        Delimiters d = Delimiters.fromIsa(raw);
        List<Segment> segments = Tokenizer.tokenize(raw, d);

        assertThat(segments).extracting(Segment::id)
                .containsExactly("ISA", "GS", "ST", "SE", "GE", "IEA");
        assertThat(segments.get(0).element(5)).isEqualTo("PREMIER");
        assertThat(segments.get(0).element(7)).isEqualTo("KEYINS");
    }

    @Test
    void tokenizerTolerantOfNewlinesBetweenSegments() {
        String raw = ISA + "\nGS*HC*S*R*20240101*1200*1*X*005010X222A1~\nST*837*0001*005010X222A1~\nSE*1*0001~\nGE*1*1~\nIEA*1*000000001~";
        Delimiters d = Delimiters.fromIsa(raw);
        assertThat(Tokenizer.tokenize(raw, d)).hasSize(6);
    }

    @Test
    void rejectsInputThatIsNotIsa() {
        assertThatThrownBy(() -> Delimiters.fromIsa("GS*HC*S*R*20240101*1200*1*X*005010X222A1~"))
                .isInstanceOf(X12ParseException.class)
                .hasMessageContaining("ISA");
    }
}
