import React from "react";
import Spinner from 'react-bootstrap/Spinner'
import {Alert} from "react-bootstrap";

function SessionLoadingError(props) {
    return <div><Alert variant="danger">Can't load session</Alert></div>;
}

export default SessionLoadingError