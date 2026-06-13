function sanitizeStringArray(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return Array.from(
    new Set(
      value
        .map((entry) => String(entry || "").trim())
        .filter(Boolean)
    )
  );
}

function toPublicUser(user) {
  return {
    id: String(user._id),
    firstName: user.firstName,
    lastName: user.lastName,
    email: user.email,
    username: user.username || null,
    researcherType: user.researcherType || null,
    institute: user.institute || null,
    department: user.department || null,
    position: user.position || null,
    gender: user.gender || null,
    avatarUrl: user.avatarUrl || null,
    coverImageUrl: user.coverImageUrl || null,
    bio: user.bio || null,
    location: user.location || null,
    website: user.website || null,
    phoneNumber: user.phoneNumber || null,
    skypeId: user.skypeId || null,
    localTime: user.localTime || null,
    disciplines: sanitizeStringArray(user.disciplines),
    skills: sanitizeStringArray(user.skills),
    createdAt: user.createdAt,
  };
}

module.exports = toPublicUser;
