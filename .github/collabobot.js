module.exports = {
    // components
    blockUserComponent: {
        enable: true,
        blockUsers: [{
            id: "xiaojinzi123",
            block: {
                openIssue: true,
                issueComment: true,
                openPullRequest: true
            },
            reason: "Public harassment"
        }]
    }
}
