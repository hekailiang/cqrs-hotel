// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import history from "../history";

function handleClick(event) {
  console.log(event);
  event.preventDefault();
  history.push({
    pathname: event.currentTarget.pathname,
    search: event.currentTarget.search
  });
}

const Link = ({to, children}) => (
  <a href={to} onClick={handleClick}>{children}</a>
);

export {Link};