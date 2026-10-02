package nbdp.trax.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketKeysTest
{
    @Test
    void addsAfterLeadingKeys()
    {
        assertThat(TicketKeys.add("home-5149 java-stack SR3 deploy", "HOME-5156"))
            .isEqualTo("home-5149 home-5156 java-stack SR3 deploy");
    }

    @Test
    void prependsWhenNoteHasNoLeadingKey()
    {
        assertThat(TicketKeys.add("calvin w/troubleshooting", "home-5156"))
            .isEqualTo("home-5156 calvin w/troubleshooting");
        assertThat(TicketKeys.add(null, "home-5156")).isEqualTo("home-5156");
        assertThat(TicketKeys.add("  ", "home-5156")).isEqualTo("home-5156");
    }

    @Test
    void addIsIdempotentAndCaseInsensitive()
    {
        String note = "HOME-5156 jspecify";
        assertThat(TicketKeys.add(note, "home-5156")).isSameAs(note);
    }

    @Test
    void matchesWholeKeysOnly()
    {
        assertThat(TicketKeys.contains("home-5156 jspecify", "home-515")).isFalse();
        assertThat(TicketKeys.add("home-5156 jspecify", "home-515"))
            .isEqualTo("home-5156 home-515 jspecify");
    }

    @Test
    void onlyLeadingKeysTagASlice()
    {
        String note = "home-5041 re-measure 410s; filed HOME-5160";

        assertThat(TicketKeys.find(note)).containsExactly("home-5041");
        assertThat(TicketKeys.contains(note, "home-5160")).isFalse();
        assertThat(TicketKeys.add(note, "home-5160"))
            .isEqualTo("home-5041 home-5160 re-measure 410s; filed HOME-5160");
        assertThat(TicketKeys.find("calvin w/home-5156")).isEmpty();
    }

    @Test
    void rejectsNonKeys()
    {
        assertThatThrownBy(() -> TicketKeys.normalize("5156")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TicketKeys.normalize("home 5156")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeKeepsKeysAddedDuringEditButNotKeysUserRemoved()
    {
        String before = "home-5149 home-5122 work";
        String current = "home-5149 home-5122 home-5156 work"; // a session tagged home-5156
        String edited = "home-5149 rewritten note";            // the user dropped home-5122

        assertThat(TicketKeys.mergeExternal(edited, before, current))
            .isEqualTo("home-5149 home-5156 rewritten note");
    }
}
