const Api = {

    getQuizes: () => {
        const requestUrl = `/api/tree/`;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    sendReponse: (quizz, path) => {
        const requestUrl = `/api/tree/` + quizz + '/path/' + path;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    sendFeedback: (rate, comment, treeId, path) => {
        // expression
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