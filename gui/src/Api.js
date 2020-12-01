const Api = {

    getTrees: () => {
        const requestUrl = `/api/tree/`;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    getSessions: () => {
        const requestUrl = '/api/tracking/sessions'
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    getSession: (session, tree) => {
        const requestUrl = `/api/tracking/session/${session}/tree/${tree}`
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    getHistoryStep: (treeId, version, path) => {
        const requestUrl = `/api/tracking/step/tree/${treeId}/version/${version}/path/${path}`;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    sendResponse: (treeId, path) => {
        const requestUrl = `/api/tree/` + treeId + '/path/' + path;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    readDemoTree: () => {
        const requestUrl = '/Demo.mup'
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.text())
    },

    addDecisionTree: (treeId, content) => {
        const requestUrl = `/api/tree/${treeId}`
        return fetch(requestUrl, {method: "PUT", body: content})
    },

    sendFeedback: (rate, comment, treeId, path) => {
        const body = {
            rate: rate,
            comment: comment,
            treeId: treeId,
            path: path
        };

        return fetch("/api/feedback", {
            headers: {
                'Content-Type': 'application/json'
            },
            method: "POST",
            body: JSON.stringify(body)
        })
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    checkStatus: response => {
        if (response.status >= 200 && response.status < 300) {
            return response;
        } else {
            // error.response = response;
            throw new Error(response.statusText);
        }
    }
}
export {Api as default};