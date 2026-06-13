const CHAT_ROLE_VALUES = ["student", "ngo", "medical", "other"];

function normalizeChatRole(value) {
  const normalized = String(value || "").trim().toLowerCase();
  return CHAT_ROLE_VALUES.includes(normalized) ? normalized : "other";
}

function formatChatRole(role) {
  switch (normalizeChatRole(role)) {
    case "student":
      return "Academic Or Student";
    case "ngo":
      return "Corporate, Govt, Or NGO Person";
    case "medical":
      return "Medical";
    default:
      return "Not a Researcher";
  }
}

module.exports = {
  CHAT_ROLE_VALUES,
  normalizeChatRole,
  formatChatRole,
};
