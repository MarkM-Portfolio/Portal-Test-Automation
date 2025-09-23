// get the button and make it respond to a click
var theButton = document.getElementById("sa_button");
theButton.onclick = feedTheButton;

// simple function compares two numbers, returns the larger one
function greatestOfTwo( first, second ) { 
	if ( first > second ) {
		return first; 
   } else {
		return second; 
  }
}

// this function runs each time the button is clicked
// the simple function is called within this one 
function feedTheButton() {
	// get the two numbers from the text input fields
	// parseInt() changes string to number 
	var box1 = parseInt(document.getElementById("sa_number1").value);
  var box2 = parseInt(document.getElementById("sa_number2").value);
  // run the function above this one and store what is returned in result 
  var result = greatestOfTwo( box1, box2 );
  // write the result into the HTML
  var place = document.getElementById("write");
  place.innerHTML = result;
}
