const followers = [
  { name: "Amy Watson", subtitle: "Bz University, Pakistan", image: "/images/resources/speak-1.jpg", actionLabel: "Follow" },
  { name: "Muhammad Khan", subtitle: "Oxford University, UK", image: "/images/resources/speak-2.jpg", actionLabel: "Follow" },
  { name: "Sadia Gill", subtitle: "Wb University, USA", image: "/images/resources/speak-3.jpg", actionLabel: "Follow" },
  { name: "Rjapal", subtitle: "Km University, India", image: "/images/resources/speak-4.jpg", actionLabel: "Follow" },
  { name: "Amy Watson", subtitle: "Oxford University, UK", image: "/images/resources/speak-5.jpg", actionLabel: "Follow" },
  { name: "Bob Frank", subtitle: "WB University, Canada", image: "/images/resources/speak-6.jpg", actionLabel: "Follow" },
];

const following = [
  { name: "Amy Watson", subtitle: "Bz University, Pakistan", image: "/images/resources/speak-10.jpg", actionLabel: "Unfollow" },
  { name: "Muhammad Khan", subtitle: "Oxford University, UK", image: "/images/resources/speak-11.jpg", actionLabel: "Unfollow" },
  { name: "Sadia Gill", subtitle: "WB University, USA", image: "/images/resources/speak-12.jpg", actionLabel: "Unfollow" },
  { name: "Rjapal", subtitle: "Km University, India", image: "/images/resources/speak-4.jpg", actionLabel: "Unfollow" },
  { name: "Amy Watson", subtitle: "Oxford University, UK", image: "/images/resources/speak-1.jpg", actionLabel: "Unfollow" },
  { name: "Bob Frank", subtitle: "WB University, Canada", image: "/images/resources/speak-2.jpg", actionLabel: "Unfollow" },
];

const suggestions = [
  { name: "Amy Watson", subtitle: "Department of Sociology", image: "/images/resources/speak-1.jpg", actionLabel: "Follow" },
  { name: "Muhammad Khan", subtitle: "Department of Sociology", image: "/images/resources/speak-2.jpg", actionLabel: "Follow" },
  { name: "Sadia Gill", subtitle: "Department of Sociology", image: "/images/resources/speak-3.jpg", actionLabel: "Follow" },
  { name: "Aykash Verma", subtitle: "Department of Sociology", image: "/images/resources/speak-4.jpg", actionLabel: "Follow" },
];

const whoIsFollowing = [
  { name: "Kelly Bill", subtitle: "Dept colleague", image: "/images/resources/friend-avatar.jpg", actionLabel: "Follow" },
  { name: "Issabel", subtitle: "Dept colleague", image: "/images/resources/friend-avatar2.jpg", actionLabel: "Follow" },
  { name: "Andrew", subtitle: "Dept colleague", image: "/images/resources/friend-avatar3.jpg", actionLabel: "Follow" },
  { name: "Sophia", subtitle: "Dept colleague", image: "/images/resources/friend-avatar4.jpg", actionLabel: "Follow" },
  { name: "Allen", subtitle: "Dept colleague", image: "/images/resources/friend-avatar5.jpg", actionLabel: "Follow" },
];

const videos = [
  { href: "https://www.youtube.com/watch?v=8iZTb9NWbz8", image: "/images/resources/user4.jpg", name: "Frank J.", meta: "1 year ago", views: "3.1k" },
  { href: "https://www.youtube.com/watch?v=8itUNRIWVIs", image: "/images/resources/user2.jpg", name: "Maria K.", meta: "2 weeks ago", views: "1.1k" },
  { href: "https://www.youtube.com/watch?v=JpxsRwnRwCQ", image: "/images/resources/user1.jpg", name: "Jack Carter", meta: "4 weeks ago", views: "20k" },
  { href: "https://www.youtube.com/watch?v=WNeLUngb-Xg", image: "/images/resources/user3.jpg", name: "Fawad Jan", meta: "1 month ago", views: "8k" },
];

const comments = [
  {
    name: "Jack Carter",
    image: "/images/resources/user1.jpg",
    time: "2 hours ago",
    message: "I think that somehow we learn who we really are and then live with that decision. Great post!",
    link: "https://www.youtube.com/watch?v=HpZgwHU1GcI",
  },
  {
    name: "Ching xang",
    image: "/images/resources/user2.jpg",
    time: "2 hours ago",
    message: "I think that somehow we learn who we really are and then live with that decision. Great post!",
  },
];

const timeline = [
  {
    id: "article-post",
    type: "article",
    authorName: "Jack Carter",
    authorImage: "/images/resources/user1.jpg",
    activity: "shared a post",
    published: "Sep 15, 2020",
    title: "Supervision as a Personnel Development Device",
    description:
      "Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero.",
    href: "post-detail.html",
  },
  {
    id: "premium-post",
    type: "premium",
    authorName: "Maria K.",
    authorImage: "/images/resources/user2.jpg",
    activity: "shared a premium product",
    published: "Sep 15, 2020",
    title: "Technical Words 2026 Book World",
    description:
      "Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero.",
    href: "book-detail.html",
    image: "/images/resources/book5.jpg",
    ctaLabel: "Buy Now",
    ctaHref: "book-detail.html",
    commentsOpen: true,
  },
  {
    id: "image-post",
    type: "image",
    authorName: "Turgut Alp",
    authorImage: "/images/resources/user3.jpg",
    activity: "created a post",
    published: "Sep 15, 2020",
    title: "Supervision as a Personnel Development Device",
    description:
      "Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero.",
    href: "post-detail.html",
    image: "/images/resources/study.jpg",
    emojiCount: "30+",
  },
  {
    id: "album-post",
    type: "album",
    authorName: "Saim Turan",
    authorImage: "/images/resources/user4.jpg",
    activity: "added an image album",
    published: "Sep 15, 2020",
    title: "Visual research notes from the latest field study",
    description:
      "Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero.",
    href: "post-detail.html",
    images: [
      "/images/resources/album1.jpg",
      "/images/resources/album2.jpg",
      "/images/resources/album6.jpg",
      "/images/resources/album5.jpg",
      "/images/resources/album4.jpg",
    ],
    morePhotosCount: 15,
    emojiCount: "50+",
  },
  {
    id: "link-post",
    type: "link",
    authorName: "Andrew Jhon",
    authorImage: "/images/resources/user5.jpg",
    activity: "shared a link",
    published: "Sep 15, 2020",
    title: "Winku Social Network with Company Pages Theme",
    description:
      "Winku is a social community mobile app kit with features for sharing blogs, posts, timeline updates, groups, pages, messages, videos and Q&A content.",
    href: "https://themeforest.net/item/winku-social-network-toolkit-responsive-template/22363538",
    image: "/images/resources/laptop.png",
    fetchedImageLabel: "fetched-image",
    commentsOpen: true,
  },
  {
    id: "video-post",
    type: "video",
    authorName: "Maria K.",
    authorImage: "/images/resources/user2.jpg",
    activity: "shared a video",
    published: "Sep 15, 2020",
    description:
      "Cookie? Biscuit? Bikkie? They all mean the same thing. This lesson compares pronunciation and vocabulary differences across Australia, America and England.",
    embedUrl: "https://www.youtube.com/embed/zdow47FQRfQ",
    emojiCount: "20+",
  },
  {
    id: "gif-post",
    type: "gif",
    authorName: "Maria K.",
    authorImage: "/images/resources/user2.jpg",
    activity: "shared a gif",
    published: "Sep 15, 2020",
    gifPreview: "/images/giphy.png",
    gifDataUrl: "/images/giphy-sample.gif",
    emojiCount: "20+",
  },
];

const researchImages = [
  "/images/resources/image1.jpg",
  "/images/resources/image2.jpg",
  "/images/resources/image3.jpg",
  "/images/resources/image4.jpg",
  "/images/resources/image5.jpg",
  "/images/resources/image6.jpg",
];

const events = [
  {
    id: "networking-night",
    title: "BZ University networking night in Columbia",
    iconClass: "icofont-gift",
    themeClass: "bg-purple",
    image: "/images/clock.png",
    href: "#",
  },
  {
    id: "conference-2026",
    title: "The 3rd International Conference 2026",
    iconClass: "icofont-microphone",
    themeClass: "bg-blue",
    image: "/images/clock.png",
    href: "#",
  },
];

module.exports = {
  followers,
  following,
  suggestions,
  whoIsFollowing,
  videos,
  comments,
  timeline,
  researchImages,
  events,
};
