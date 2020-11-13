const Api = {

    getQuizes: () => {
        const requestUrl = `/api/quiz/`;
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    sendReponse: (quizz, path) => {
        const requestUrl = `/api/quiz/` + quizz + '/path/' + path;    
        return fetch(requestUrl)
            .then(Api.checkStatus)
            .then(response => response.json())
    },

    sendFeedback: (rate, comment, quizzId, path) => {
        // expression
        const body = {
            rate: rate,
            comment: comment,
            quizzId: quizzId,
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